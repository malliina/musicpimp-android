package org.musicpimp.subsonic

import java.util.concurrent.ScheduledFuture

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.Scheduling
import org.musicpimp.audio._
import org.musicpimp.exceptions.SubsonicHttpException
import org.musicpimp.http.Endpoint
import org.musicpimp.subsonic.SubsonicPlayer._

import scala.concurrent.Future
import scala.concurrent.duration._

class SubsonicPlayer(val endpoint: Endpoint)
  extends Player
    with SubsonicHttpClient
    with SelfSubscription {
//  val client = PimpOkClient.subsonic(endpoint)
  private var poller: Option[ScheduledFuture[_]] = None
  var preMuteVolume: Option[Int] = None
  val json = new SubsonicJsonReaders(endpoint)

  def onPlayerEvent(event: PlayerEvent) = event match {
    case e: StatusEvent =>
      // decomposes the status event and fires state change events if necessary
      val oldStatus = playerStatus
      playerStatus = e
      if (e.position.toSeconds != oldStatus.position.toSeconds)
        fireEvent(TimeUpdated(e.position))
      if (e.mute != oldStatus.mute)
        fireEvent(MuteToggled(e.mute))
      if (e.volume != oldStatus.volume)
        fireEvent(VolumeChanged(e.volume))
      val newTrack = e.track
      if (newTrack != oldStatus.track && newTrack.isDefined)
        fireEvent(TrackChanged(newTrack))
      if (e.state != oldStatus.state)
        fireEvent(PlayStateChanged(e.state))
      if (e.playlist != oldStatus.playlist)
        fireEvent(PlaylistModified(e.playlist))
      val newIndex = e.playlistIndex
      if (newIndex != oldStatus.playlistIndex)
        fireEvent(PlaylistIndexChanged(newIndex))
    case _ => ()
  }

  def add(track: Track): Unit = action(ADD, ID -> track.id)

  def remove(index: Int): Unit = action(REMOVE, INDEX -> index.toString)

  def skip(index: Int): Unit =
    if (isPlaybackAllowed) action(SKIP, INDEX -> index.toString)
    else sendLimitExceededMessage()

  def setAndPlay(track: Track): Unit = {
    // subsonic may have a 'set' method
    action(CLEAR)
    add(track)
    resume()
  }

  def resume(): Unit = action(START)

  def pause(): Unit = action(STOP)

  /** Implementation notes: The seeking action in Subsonic is called "skip",
    * so there is no typo here.Notice from the API that you need to specify
    * the playlist index (index) in addition to the track position (offset)
    * when seeking.
    *
    * @param pos track position to seek to
    */
  def seek(pos: Duration): Unit = action(
    SKIP,
    INDEX -> (index getOrElse 0).toString,
    OFFSET -> pos.toSeconds.toString)

  def playNext(): Unit = skipTo(_ + 1)

  def playPrevious(): Unit = skipTo(i => if (i > 0) i - 1 else 0)

  def skipTo(f: Int => Int): Unit =
    skip(index.fold(0)(f))

  def mute(muted: Boolean): Unit = {
    if (muted) {
      preMuteVolume = Some(playerStatus.volume)
      volume(0)
    } else {
      preMuteVolume.foreach(volume)
      preMuteVolume = None
    }
  }

  /** Subsonic expects a value between [0, 1.0] for the gain.
    *
    * @param volume [0, 100]
    */
  def volume(volume: Int): Unit = action(SET_GAIN, GAIN -> (1.0f * volume / 100).toString)

  private def action(action: String, additionalParams: (String, String)*): Future[Unit] =
    jukeboxControl(Seq(ACTION -> action) ++ additionalParams)

  private def jukeboxControl(queryParams: Seq[(String, String)]): Future[Unit] = {
    val path = buildPath(queryParams: _*)
    //    info(s"Calling jukebox control: $path")
    client.getEmpty(path)
  }

  private def buildPath(queryParams: (String, String)*): String =
    SubsonicHttpClient.buildPath("jukeboxControl", queryParams: _*)

  override def startPolling() {
    val task = Scheduling.every(3000.milliseconds) {
      val statusFuture = client.getJson[StatusEvent](buildPath(ACTION -> GET))(json.statusReader)
      statusFuture.map(fireEvent).onFailure {
        case she: SubsonicHttpException =>
          she.reasonOpt.filter(_ == "ConcurrentModificationException").fold(stopBecause(she))(msg => {
            // the server has some issues with concurrent requests, suppress and try again
          })
        case e: Exception =>
          stopBecause(e)
      }
    }
    poller = Some(task)
  }

  def stopBecause(cause: Exception) {
    //    warn(s"Unable to poll Subsonic server jukebox for status", cause)
    stopPolling()
  }

  override def stopPolling() {
    poller.foreach(_.cancel(true))
    poller = None
  }
}

object SubsonicPlayer {
  val ACTION = "action"
  val START = "start"
  val STOP = "stop"
  val ID = "id"
  val SKIP = "skip"
  val OFFSET = "offset"
  val INDEX = "index"
  val CLEAR = "clear"
  val ADD = "add"
  val REMOVE = "remove"
  val STATUS = "status"
  val SET = "set"
  val SET_GAIN = "setGain"
  val GAIN = "gain"
  val GET = "get"
}
