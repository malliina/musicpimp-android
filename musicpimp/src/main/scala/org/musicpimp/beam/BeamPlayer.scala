package org.musicpimp.beam

import com.mle.android.exceptions.{ExplainedException, ExplainedHttpException}
import com.mle.android.http.HttpResponse
import com.mle.util.Utils.executionContext
import org.apache.http.client.HttpResponseException
import org.musicpimp.audio._
import org.musicpimp.beam.BeamPlayer._
import org.musicpimp.exceptions.{BeamPlayerNotFoundException, ConcurrentStreamingException}
import org.musicpimp.http.Endpoint
import org.musicpimp.json.JsonStrings._
import org.musicpimp.util.Messaging

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, DurationLong}


object BeamPlayer {
  val wsResource = "/ws/control"
  val streamResource = "/stream"
  val addToPlaylistResource = "/stream/tail"
  val streamableResource = "/streamable"
  val disconnectedMessage = "The MusicBeamer endpoint disconnected. Attempts to control playback will likely fail. You might want to adjust your settings."
}

/**
 * TODO status is cached in two places: playbackStatus and LocalPlaylist impl
 * @author mle
 */
class BeamPlayer(endpoint: Endpoint)
  extends PimpWebSocketPlayer(endpoint, BeamPlayer.wsResource)
  with SelfSubscription
  with PimpHttpClient {

  override val supportsSeekAndSkip = false

  var position: Duration = 0.seconds

  override protected def onPlayerEvent(event: PlayerEvent): Unit = event match {
    case Welcomed =>
      sendSimple(CONNECTED)
    case Disconnected(user) if user == endpoint.username =>
      Messaging send disconnectedMessage
    case TimeUpdated(newTime) =>
      playerStatus = playerStatus.copy(position = newTime)
    case PlayStateChanged(newState) =>
      playerStatus = playerStatus.copy(state = newState)
    case PlaylistModified(list) =>
      playerStatus = playerStatus.copy(playlist = list)
    case VolumeChanged(newVol) =>
      playerStatus = playerStatus.copy(volume = newVol)
    case MuteToggled(newMute) =>
      playerStatus = playerStatus.copy(mute = newMute)
    case ShortStatusEvent(state, pos, muted, volume) =>
      playerStatus = playerStatus.copy(state = state, position = pos, mute = muted, volume = volume)
    case PlaylistIndexChanged(newIndex) =>
      playerStatus = playerStatus.copy(playlistIndex = newIndex)
    case TrackChanged(newTrack) =>
      playerStatus = playerStatus.copy(track = newTrack)
  }

  def handleTimeUpdated(event: TimeUpdated) {
    val time = event.position
    val currentTrackPosition = trackTime(time)
    if (playerStatus.position != currentTrackPosition)
      fireEvent(TimeUpdated(currentTrackPosition))

    val idx = calculateIndex(time)
    if (idx != playerStatus.playlistIndex) {
      fireEvent(PlaylistIndexChanged(idx))
      fireEvent(TrackChanged(currentTrack))
    }
  }

  override def onWebSocketEvent(event: PlayerEvent): Unit =
    event match {
      case tue: TimeUpdated => handleTimeUpdated(tue)
      case _ => super.onWebSocketEvent(event)
    }

  private def trackTime(position: Duration): Duration =
    calculateIndex(position).fold(position)(idx => {
      val previousTrackDurationCombined = tracks.take(idx).map(_.duration.toSeconds).sum
      (position.toSeconds - previousTrackDurationCombined).seconds
    })

  /**
   *
   * @param position the total playback time since the playlist was reset
   * @return the playlist index
   */
  def calculateIndex(position: Duration): Option[Int] = {
    val durations = tracks.map(_.duration.toSeconds)
    val pos = position.toSeconds
    var acc = 0L
    if (durations.size == 0) {
      None
    } else {
      Some(durations.takeWhile(dur => {
        acc += dur
        acc < pos
      }).size)
    }
  }

  override def remove(index: Int): Unit = {}

  def skip(position: Int): Unit = {}

  def setAndPlay(track: Track): Unit =
    withErrorHandling(track) {
      streamable.flatMap(status => {
        if (!status.exists) {
          Future.failed[HttpResponse](new BeamPlayerNotFoundException)
        } else {
          set(track)
          stream(track, endpoint, streamResource)
        }
      })
    }

  def set(track: Track): Unit = {
    fireEvent(PlaylistModified(Seq(track)))
    fireEvent(PlaylistIndexChanged(Some(0)))
    fireEvent(TrackChanged(Some(track)))
  }

  def add(track: Track): Unit =
    withErrorHandling(track) {
      streamable.flatMap(status => {
        if (!status.exists) {
          throw new BeamPlayerNotFoundException
        } else if (!status.ready) {
          throw new ConcurrentStreamingException(track)
        } else {
          fireEvent(PlaylistModified(playerStatus.playlist :+ track))
          stream(track, endpoint, addToPlaylistResource)
        }
      })
    }

  def withErrorHandling[T](track: Track)(f: => Future[T]) =
    f.onFailure(parseErrorMessage(track) andThen Messaging.send)

  private def parseErrorMessage(track: Track): PartialFunction[Throwable, String] = {
    //    case nsee: NoSuchElementException =>
    //      // NoSuchElementException is thrown if the predicate fails in Future.filter(predicate)
    //      s"The connection to the MusicBeamer player has been lost. Please review your playback settings."
    case cse: ConcurrentStreamingException =>
      cse.getMessage
    case bpnfe: BeamPlayerNotFoundException =>
      bpnfe.getMessage
    case phe: ExplainedHttpException =>
      phe.reason
    case hre: HttpResponseException =>
      "A network error occurred. Please check your settings and try again."
    case pe: ExplainedException =>
      pe.getMessage
    case usoe: UnsupportedOperationException =>
      usoe.getMessage
    case e: Exception =>
      //      warn("MusicBeamer error", e)
      val reason = Option(e.getMessage) getOrElse ""
      s"Unable to stream ${track.title} to MusicBeamer. $reason"
  }

  private def stream(track: Track, e: Endpoint, path: String): Future[HttpResponse] = {
    val uri = e httpUri path
    //    info(s"Streaming $track to $uri...")
    val request = TrackUploadRequest(track.id, uri, e.username, e.password)
    val ret = LibraryManager.active.upload(request)
    ret.foreach(_ => {
      //      info(s"Uploaded $track to MusicBeamer at URI $uri")
    })
    ret
  }

  def seek(pos: Duration): Unit = {}

  def playNext(): Unit = {}

  def playPrevious(): Unit = {}

  private def streamable = client.getJson[BeamStatus](streamableResource)
}

