package org.musicpimp.usage

import org.musicpimp.audio.{TrackChanged, PlayerEvent, Track, Player}
import org.musicpimp.util.PimpLog
import scala.concurrent.duration.Duration

/**
 *
 * @author mle
 */
trait LocalPlayerLimiter extends ExtendedPlayAndSkipLimiter

trait PimpLimiter extends ExtendedPlayAndSkipLimiter

trait SubsonicLimiter extends PlayAndSkipLimiter

trait BeamLimiter extends PlaybackLimiter

trait ExtendedPlayAndSkipLimiter extends PlayAndSkipLimiter {

  abstract override def playNext(): Unit =
    withLimitControl(super.playNext())

  abstract override def playPrevious(): Unit =
    withLimitControl(super.playPrevious())
}

trait PlayAndSkipLimiter extends PlaybackLimiter {
  abstract override def skip(index: Int) =
    withLimitControl(super.skip(index))
}

/**
 * Monitors playback and limits it as appropriate.
 *
 * This trait does two things: a) it informs [[org.musicpimp.usage.PimpUsageController]]
 * every time a track changes and b) it ensures that certain implementations are called
 * only if playback is allowed, using the stackable traits pattern. The result is that
 * certain functionality is disabled if this limiter is mixed in with a
 * [[org.musicpimp.audio.Player]] and the playback limit has been reached; subtraits may
 * limit playback even further as they see fit.
 *
 */
trait PlaybackLimiter extends Player with PimpLog {
  events.subscribe(e => onEvent(e))

  abstract override def add(track: Track): Unit =
    withLimitControl(super.add(track))

  abstract override def add(tracks: Seq[Track]): Unit =
    withLimitControl(super.add(tracks))

  abstract override def remove(index: Int): Unit =
    withLimitControl(super.remove(index))

  abstract override def setAndPlay(track: Track): Unit =
    withLimitControl(super.setAndPlay(track))

  // prevents the playlist from advancing if the limit is hit
  abstract override def next: Option[Track] =
    super.next.filter(_ => isPlaybackAllowed)

  abstract override def seek(pos: Duration) =
    withLimitControl(super.seek(pos))

  /**
   * Wrapping `f` in this method ensures that if the playback limit has been reached,
   * `f` is not evaluated but instead a [[org.musicpimp.util.PlaybackLimitExceeded]]
   * message is sent. The UI should then display an [[org.musicpimp.ui.dialogs.IapDialog]]
   * to the user, suggesting they upgrade to the premium version.
   *
   * @param f code to run on the condition that playback is allowed
   */
  protected def withLimitControl(f: => Any): Unit =
    if (isPlaybackAllowed) {
      f
    } else {
      info("Refusing player action because the playback limit has been reached.")
      sendLimitExceededMessage()
    }

  override def isPlaybackAllowed: Boolean =
    if (isLocal) PimpUsageController.isLocalPlaybackAllowed
    else PimpUsageController.isRemotePlaybackAllowed

  private def onEvent(event: PlayerEvent) = event match {
    case TrackChanged(track) if track.isDefined =>
      if (isLocal) PimpUsageController.localPlaybackStarted()
      else PimpUsageController.remotePlaybackStarted()
    case _ => ()
  }
}
