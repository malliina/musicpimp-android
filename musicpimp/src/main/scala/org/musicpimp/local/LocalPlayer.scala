package org.musicpimp.local

import android.content.{Context, Intent}
import android.media.MediaPlayer
import com.mle.util.Scheduling
import java.util.concurrent.ScheduledFuture
import org.musicpimp.PimpApp
import org.musicpimp.audio._
import org.musicpimp.usage.LocalPlayerLimiter
import scala.concurrent.duration._
import scala.util.Try

/**
 * Media player that delegates calls by sending intents to the background
 * audio <code>Service</code>.
 *
 * The constructor is protected as evident from the uncommon syntax.
 *
 * @author mle
 */
class LocalPlayer protected() extends Player with LocalPlaylist {
  override val isLocal = true
  private val DEFAULT_VOLUME = 100
  private var isMuted = false
  private var vol = DEFAULT_VOLUME
  // polls player for time updates
  private var poller: Option[ScheduledFuture[_]] = None
  private var mPlayer: Option[MediaPlayer] = None

  private var playerTrack: Option[Track] = None

  def track = playerTrack

  def track_=(newTrack: Option[Track]) {
    playerTrack = newTrack
    fireEvent(TrackChanged(newTrack))
  }

  def mediaPlayer = mPlayer

  /**
   * Called whenever a track changes. Reinstalls the previous volume & mute state.
   *
   * @param player a player with new a track
   */
  def initMediaPlayer(player: MediaPlayer) {
    mPlayer = Some(player)
    volume(vol)
    mute(isMuted)
  }

  def closeMediaPlayer() {
    mPlayer = None
    fireEvent(PlayStateChanged(PlayStates.NoMedia))
    stopPollingForTimeUpdates()
  }

  def setAndPlay(track: Track) {
    // sets playlist
    set(track)
    // starts background media player
    startIntent(MediaService.RESTART_ACTION)
    //    info(s"Starting playback of: ${track.title}")
  }

  def resume() {
    mediaPlayer.foreach(startOrResume)
  }

  private def startOrResume(mp: MediaPlayer): Unit = {
    mp.start()
    fireEvent(PlayStateChanged(PlayStates.Started))
    startPollingForTimeUpdates()
    track.foreach(t => updateNotification(t, playing = true))
  }

  def pause() {
    val shouldUpdateNotification = isPlaying
    stopPlayback()
    track.foreach(t => {
      if (shouldUpdateNotification) {
        updateNotification(t, playing = false)
      }
    })
  }

  private def cancelNotification(): Unit =
    new Notifications(PimpApp.context).cancel()

  private def updateNotification(track: Track, playing: Boolean): Unit =
    new Notifications(PimpApp.context).displayTrackNotification(track, playing)

  // MediaPlayer.isPlaying may throw IllegalStateException (hahaha)
  private def isPlaying: Boolean =
    mediaPlayer.exists(p => Try(p.isPlaying).filter(_ == true).isSuccess)

  def stop() {
    stopPlayback()
    cancelNotification()
  }

  private def stopPlayback() {
    mediaPlayer.foreach(p => {
      p.pause()
      fireEvent(PlayStateChanged(PlayStates.Stopped))
      stopPollingForTimeUpdates()
    })
  }

  def seek(pos: Duration) {
    mediaPlayer.foreach(_ seekTo pos.toMillis.toInt)
  }

  def playNext() {
    startIntent(MediaService.NEXT_ACTION)
  }

  def playPrevious() {
    startIntent(MediaService.PREV_ACTION)
  }

  def skip(position: Int) {
    index = Some(position)
    startIntent(MediaService.PLAY_PLAYLIST)
  }

  private def startIntent(action: String) {
    val context = PimpApp.context
    val intent = playbackIntent(context, action)
    context.startService(intent)
  }

  private def playbackIntent(ctx: Context, action: String) = {
    val intent = new Intent(ctx, classOf[MediaService])
    intent setAction action
    intent
  }

  def mute(muted: Boolean) {
    mediaPlayer.foreach(player => {
      isMuted = muted
      // ???
      //      if (muted) {
      //        vol = streamVolume
      //      }
      val floatVolume = if (muted) 0.0f else floatify(vol)
      player.setVolume(floatVolume, floatVolume)
      fireEvent(MuteToggled(muted))
    })
  }

  /**
   *
   * @param volume [0, 100]
   */
  def volume(volume: Int) {
    mediaPlayer.foreach(player => {
      vol = volume
      val floatVolume = floatify(volume)
      player.setVolume(floatVolume, floatVolume)
      fireEvent(VolumeChanged(volume))
    })
  }

  /**
   * @return the current volume as an integer within [0, 100]
   */
  //  def streamVolume: Int = {
  //    val am = PimpApp.context.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]
  //    val vol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
  //    val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
  //    (100.0 * vol / maxVolume).toInt
  //  }
  // map from [0, 100] to [0.0f, 1.0f]
  def floatify(volume: Int) = 1.0f * volume / 100

  def onPrepared(mp: MediaPlayer): Unit = {
    startOrResume(mp)
  }

  def status: StatusEvent = mediaPlayer.fold(StatusEvent.empty)(player => {
    val state = if (isPlaying) PlayStates.Started else PlayStates.Stopped
    val pos = player.getCurrentPosition.milliseconds
    StatusEvent(currentTrack, state, pos, vol, isMuted, tracks, index)
  })


  private def startPollingForTimeUpdates() {
    val task = Scheduling.every(900 milliseconds) {
      mediaPlayer.foreach(player => {
        fireEvent(TimeUpdated(player.getCurrentPosition.milliseconds))
      })
    }
    poller = Some(task)
  }

  private def stopPollingForTimeUpdates() {
    poller.foreach(_.cancel(true))
    poller = None
  }
}

object LocalPlayer extends LocalPlayer

object LimitedLocalPlayer extends LocalPlayer with LocalPlayerLimiter