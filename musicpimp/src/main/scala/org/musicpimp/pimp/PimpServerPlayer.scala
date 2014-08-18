package org.musicpimp.pimp

import com.mle.android.http.{AuthHttpClient, HttpResponse}
import com.mle.util.Utils.executionContext
import concurrent.duration._
import org.musicpimp.audio._
import org.musicpimp.http.Endpoint
import org.musicpimp.json.JsonStrings
import org.musicpimp.json.JsonStrings._
import play.api.libs.json.Json
import scala.concurrent.Future

/**
 * TODO take Rx into use.
 *
 * @author mle
 */
class PimpServerPlayer(endpoint: Endpoint)
  extends PimpWebSocketPlayer(endpoint, PimpServerPlayer.wsResource)
  with Player
  with SelfSubscription
  with PimpHttpClient {

  override protected def onPlayerEvent(event: PlayerEvent) = event match {
    case Welcomed =>
      sendSimple(STATUS)
    case e: StatusEvent =>
      playerStatus = e
      fireInitialStatusEvents(e)
    case TimeUpdated(time) =>
      playerStatus = playerStatus.copy(position = time)
    case TrackChanged(newTrack) =>
      playerStatus = playerStatus.copy(track = newTrack)
    case PlaylistModified(list) =>
      playerStatus = playerStatus.copy(playlist = list)
    case PlaylistIndexChanged(i) =>
      playerStatus = playerStatus.copy(playlistIndex = i)
    case PlayStateChanged(newState) =>
      playerStatus = playerStatus.copy(state = newState)
    case VolumeChanged(newVol) =>
      playerStatus = playerStatus.copy(volume = newVol)
    case MuteToggled(newMute) =>
      playerStatus = playerStatus.copy(mute = newMute)
    case _ => ()
  }

  /**
   * Updates any listeners with the player state following the reception of a status event.
   *
   * @param status the player state
   */
  protected def fireInitialStatusEvents(status: StatusEvent) {
    fireEvent(TrackChanged(status.track))
    fireEvent(PlayStateChanged(status.state))
    fireEvent(PlaylistModified(status.playlist))
    fireEvent(PlaylistIndexChanged(status.playlistIndex))
    fireEvent(VolumeChanged(status.volume))
    fireEvent(MuteToggled(status.mute))
  }

  def setAndPlay(track: Track): Unit = {
    if (LibraryManager.active.isLocal) {
      upload(track)
    } else {
      sendTrack(PLAY, track)
    }
  }

  def seek(pos: Duration): Unit = sendValued(SEEK, pos.toSeconds)

  def add(track: Track): Unit = sendTrack(ADD, track)

  def remove(index: Int): Unit = sendValued(REMOVE, index)

  def skip(position: Int): Unit = sendValued(SKIP, position)

  def playNext(): Unit = sendSimple(NEXT)

  def playPrevious(): Unit = sendSimple(PREV)

  def upload(track: Track): Future[HttpResponse] = {
    val file = LibraryManager.localLibrary.path(track)
    val httpClient = new AuthHttpClient(endpoint.username, endpoint.password)
    httpClient.httpClient setTimeout (6 minutes).toMillis.toInt
    httpClient.addHeaders(JsonStrings.TRACK_CAPITAL_T -> Json.stringify(Json.toJson(track)(TrackHelp.json)))
    val ret = httpClient.postFile(endpoint httpUri PimpServerPlayer.streamResource, file)
    ret.onComplete(_ => httpClient.close())
    ret
  }
}

object PimpServerPlayer {
  val (wsResource, streamResource) = ("/ws/playback", "/playback/server")
}

case class TrackCommand(cmd: String, track: String)

object TrackCommand {
  implicit val json = Json.format[TrackCommand]
}

case class ValueCommand[T](cmd: String, value: T)

case class SimpleCommand(cmd: String)

object SimpleCommand {
  implicit val json = Json.format[SimpleCommand]
}
