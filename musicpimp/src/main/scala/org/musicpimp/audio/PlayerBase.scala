package org.musicpimp.audio

import com.malliina.json.{JsonFormats, SimpleFormat}
import java.io.Closeable
import org.musicpimp.util.Messaging
import play.api.libs.json.Json
import rx.lang.scala.{Subject, Observable}
import scala.concurrent.Future
import scala.concurrent.duration._

trait PlayerBase extends Closeable {
  protected val eventsSubject = Subject[PlayerEvent]()

  val events: Observable[PlayerEvent] = eventsSubject

  protected def fireEvent(event: PlayerEvent): Unit = {
    eventsSubject onNext event
  }

  def isLocal: Boolean = false

  def supportsSeekAndSkip = true

  def isPlaybackAllowed = true

  def sendLimitExceededMessage(): Unit = Messaging.limitExceeded()

  /**
   * Initializes the playlist with the given track and starts playback from the beginning.
   *
   * Any previous playlist is discarded.
   *
   * @param track track to play
   */
  def setAndPlay(track: Track): Unit //: Future[Unit]

  /**
   * Resumes or starts playback.
   */
  def resume(): Unit // : Future[Unit]

  /**
   * Pauses playback.
   *
   * Remembers the current track position for subsequent calls to <code>resume()</code>.
   */
  def pause() // : Future[Unit]
  /**
   * Convenience that delegates to either `resume()` or `pause()`.
   */
  def playOrPause(): Unit = status.state match {
    case PlayStates.Playing | PlayStates.Started => pause()
    case _ => resume()
  }

  def seek(pos: Duration) //: Future[Unit]

  /**
   * Starts playback of the next track in the playlist.
   *
   * Noops if no next track exists.
   */
  def playNext() //: Future[Unit]

  /**
   * Starts playback of the previous playlist track.
   *
   * Noops if no previous track exists.
   */
  def playPrevious() //: Future[Unit]

  def mute(muted: Boolean) // : Future[Unit]

  /**
   *
   * @param volume [0, 100]
   */
  def volume(volume: Int) // : Future[Unit]

  def status: StatusEvent

  def playing = status.state == PlayStates.Playing || status.state == PlayStates.Started

  /**
   * Opens the player. Typically used by remote players to open websocket connections or whatnot, can no-op for local
   * players.
   */
  def open(): Future[Unit] = Future.successful()

  /**
   * Closes the player. Closes any remote connections etc.
   *
   * Whether it is fine to reuse this player instance after a call to this method
   * is up to the implementation.
   */
  def close(): Unit = {}

  /**
   * Starts polling the source player in order to maintain playback state.
   *
   * This is only needed for Subsonic server playback, other implementations are event-based and shall not poll here, so
   * the default implementation no-ops. The local android player also polls for time updates, but at different times and
   * is therefore handled elsewhere.
   *
   * @see [[org.musicpimp.local.LocalPlayer]]
   * @see [[org.musicpimp.subsonic.SubsonicPlayer]]
   */
  def startPolling() {}

  def stopPolling() {}
}


// copied from MusicPimp server project, TODO move to util-pimp module
object PlayStates extends Enumeration {
  type PlayState = Value
  // some of these enums overlap and remain for API compatibility reasons for now
  // TODO: remove Started and NotPlaying in favor of Playing and Stopped respectively
  val Unrealized, Realizing, Realized,
  Prefetching, Prefetched, NoMedia,
  Open, Started, Stopped,
  Closed, Unknown, Playing,
  Paused, NotPlaying, Buffering,
  Emptied, Seeking, Seeked,
  Waiting, Ended = Value

  def withNameIgnoreCase(name: String) = values.find(_.toString.toLowerCase == name.toLowerCase)
    .getOrElse(throw new NoSuchElementException(s"Unknown playstate: $name"))

  // Stopped, Started, Buffering, Emptied, Emptied, Seeking, Seeked,
  // Waiting, Ended used by: MusicBeamer (html5 audio element)

  implicit object playStateFormat extends SimpleFormat[PlayStates.PlayState](withNameIgnoreCase)

}

// TODO Rx

trait PlayerEvent

case object Welcomed extends PlayerEvent

case class Disconnected(user: String) extends PlayerEvent

object Disconnected {
  implicit val json = Json.format[Disconnected]
}

case class PlayStateChanged(state: PlayStates.PlayState) extends PlayerEvent

case class TimeUpdated(position: Duration) extends PlayerEvent

case class TrackChanged(track: Option[Track]) extends PlayerEvent

case class PlaylistModified(playlist: Seq[Track]) extends PlayerEvent

case class PlaylistIndexChanged(index: Option[Int]) extends PlayerEvent

case class VolumeChanged(volume: Int) extends PlayerEvent

case class MuteToggled(mute: Boolean) extends PlayerEvent

case object SuspendedGettingData extends PlayerEvent

case class StatusEvent(track: Option[Track],
                       state: PlayStates.PlayState,
                       position: Duration,
                       volume: Int,
                       mute: Boolean,
                       playlist: Seq[Track],
                       playlistIndex: Option[Int]) extends PlayerEvent {
  def isPlaying = state == PlayStates.Playing || state == PlayStates.Started
}

object StatusEvent {
  val empty = StatusEvent(
    None,
    PlayStates.Closed,
    Duration.fromNanos(0),
    40,
    mute = false,
    playlist = Seq.empty[Track],
    playlistIndex = None
  )
}

case class ShortStatusEvent(state: PlayStates.PlayState,
                            position: Duration,
                            mute: Boolean,
                            volume: Int) extends PlayerEvent

object ShortStatusEvent {

  implicit val durationFormat = JsonFormats.duration
  implicit val jsonFormat = Json.format[ShortStatusEvent]
}

