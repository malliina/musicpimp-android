package org.musicpimp.local

import android.app.Service
import android.content.{ComponentName, Context, Intent, IntentFilter}
import android.media.{AudioManager, MediaPlayer}
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.{Build, Handler, IBinder, PowerManager}
import android.widget.Toast
import com.mle.android.http.HttpConstants
import org.musicpimp.audio.{PlayerManager, Track}
import org.musicpimp.ui.receivers.{MusicIntentReceiver, RemoteControlReceiver}

import scala.util.Try

/**
 * A background audio player. An implementation of:
 * http://developer.android.com/guide/topics/media/mediaplayer.html
 * http://developer.android.com/training/managing-audio/index.html
 *
 * This media player is solely controlled by sending <code>Intent</code>s to it, which are
 * received and handled in <code>onStartCommand(Intent,Int,Int)</code>.
 *
 * No methods of this class are called directly from other classes.
 * However, this class may call any other methods in the app as the background player
 * will run in the same process as the main app.
 */
class MediaService
  extends Service
  with MediaPlayer.OnPreparedListener
  with MediaPlayer.OnErrorListener
  with MediaPlayer.OnCompletionListener
  with AudioManager.OnAudioFocusChangeListener
  with MediaPlayer.OnInfoListener {

  import MediaService._

  val localPlayer = PlayerManager.localPlayer

  var wifiLock: Option[WifiManager#WifiLock] = None

  val mediaButtonBroadcastReceiver = new ComponentName("org.musicpimp", classOf[RemoteControlReceiver].getName)

  val intentReceiver = new MusicIntentReceiver
  // filter that captures events when bluetooth is disconnected, headplug is removed, etc, allowing us to pause playback
  val noisyAudioIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

  var resumeOnAudioFocusGain = false
  var isDucking = false

  def audioManager = getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]

  private var handlerOpt: Option[Handler] = None

  override def onStartCommand(intent: Intent, flags: Int, startId: Int): Int = {
    handlerOpt = Some(new Handler())
    Option(intent).fold({
      // intent might be null. when? what to do?
    })(i => {
      //      info(s"Service got start command: ${i.getAction}")
      i.getAction match {
        case RESTART_ACTION =>
          localPlayer.currentTrack.foreach(play)
        case PLAY_PLAYLIST =>
          localPlayer.currentTrack.foreach(play)
        //        case PLAY_ACTION =>
        //          // resume if the player has a track, otherwise start with the current playlist track
        //          LocalPlayer.mediaPlayer.map(_.start()).getOrElse(LocalPlayer.currentTrack.map(play))
        case NEXT_ACTION =>
          localPlayer.toNext.foreach(play)
        case PREV_ACTION =>
          localPlayer.toPrevious.foreach(play)
        case RESUME_ACTION =>
          localPlayer.resume()
        case PAUSE_ACTION =>
          localPlayer.pause()
        case CLOSE_ACTION =>
          localPlayer.stop()
        case other =>
        //          warn(s"Unknown playback action: $other")
      }

    })

    /**
     *
     * http://developer.android.com/guide/components/services.html
     *
     * START_STICKY If the system kills the service after onStartCommand() returns,
     * recreate the service and call onStartCommand(), but do not redeliver the last intent.
     * Instead, the system calls onStartCommand() with a null intent,
     * unless there were pending intents to start the service, in which case,
     * those intents are delivered.
     *
     * This is suitable for media players (or similar services) that are not executing commands,
     * but running indefinitely and waiting for a job.
     */
    Service.START_STICKY
  }

  def play(t: Track) {
    localPlayer.track = Some(t)
    initMediaPlayer(t)
    registerReceiver(intentReceiver, noisyAudioIntentFilter)
  }

  def initMediaPlayer(track: Track) {
    // prevents concurrent playback of multiple tracks...
    closePlayer()
    // does nothing without audio focus: I suppose some other app is playing music then
    if (requestAudioFocus) {
      val ctx = getApplicationContext
      // keeps wifi on in the background in case we're streaming over the network
      // improvement suggestion: check that we're streaming over wlan specifically
      val trackUri = track.source
      if (trackUri.isAbsolute && trackUri.getScheme == "http" || trackUri.getScheme == "https") {
        // getOrElseUpdate
        val wifi = wifiLock.getOrElse {
          val lock = createWifiLock
          wifiLock = Some(lock)
          lock
        }
        wifi.acquire()
      }
      val player = new MediaPlayer
      localPlayer.initMediaPlayer(player)
      audioManager.registerMediaButtonEventReceiver(mediaButtonBroadcastReceiver)
      // keeps cpu alive to enable background audio playback
      player.setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
      player setOnPreparedListener this
      player setOnCompletionListener this
      player setOnErrorListener this
      player setAudioStreamType AudioManager.STREAM_MUSIC

      if (Build.VERSION.SDK_INT < 14 && track.username.nonEmpty) {
        val uri =
          if (track.username.nonEmpty) {

            // TODO test this

            // injects the credentials into the query parameters because custom HTTP headers are not supported
            val trackUriString = trackUri.toString
            val prefix = if (trackUriString contains "?") "&" else "?"
            val queryParams = s"${prefix}u=${track.username}&p=${track.password}&s=${track.cloudID.getOrElse("")}"
            val uriWithQueryParams = trackUriString + queryParams
            Uri.parse(uriWithQueryParams)
          } else {
            trackUri
          }
        player.setDataSource(ctx, uri)
      } else {
        val headers = Map(
          HttpConstants.AUTHORIZATION -> track.authValue
        )
        // The overload with headers was added in API level 14
        player.setDataSource(ctx, trackUri, collection.JavaConversions.mapAsJavaMap(headers))
      }
      // calls onPrepared(MediaPlayer) when ready
      player.prepareAsync()
    }
  }

  /**
   * Requests audio focus.
   *
   * @return true if this media player was granted audio focus, false otherwise
   */
  def requestAudioFocus: Boolean = {
    import AudioManager._
    val result = audioManager.requestAudioFocus(this, STREAM_MUSIC, AUDIOFOCUS_GAIN)
    result == AUDIOFOCUS_REQUEST_GRANTED
  }

  def onPrepared(mp: MediaPlayer): Unit = {
    localPlayer.onPrepared(mp)
  }

  def createWifiLock = {
    val wifi = getSystemService(Context.WIFI_SERVICE).asInstanceOf[WifiManager]
      .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG)
    wifi setReferenceCounted false
    wifi
  }

  def onCompletion(mp: MediaPlayer): Unit =
    localPlayer.toNext.fold(closePlayer())(play)

  def onError(mp: MediaPlayer, what: Int, extra: Int): Boolean = {
    val desc = describeError(what)
    //    warn(s"A media playback error occurred. $desc.")
    showToast("Unable to play track. Please try another track.")
    closePlayer()
    true
  }

  def describeError(what: Int): String = {
    import MediaPlayer._
    what match {
      //      case MEDIA_ERROR_IO => "IO error"
      //      case MEDIA_ERROR_MALFORMED => "Malformed bitstream"
      case MEDIA_ERROR_SERVER_DIED => "Server died"
      //      case MEDIA_ERROR_TIMED_OUT => "Timed out"
      //      case MEDIA_ERROR_UNSUPPORTED => "Unsupported bitstream"
      case MEDIA_ERROR_UNKNOWN => "Unknown"
      case _ => "Unknown error"
    }
  }

  def onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean = {
    //    info(s"Media info: $what")
    true
  }

  def onAudioFocusChange(focusChange: Int) {
    focusChange match {
      case AudioManager.AUDIOFOCUS_GAIN =>
        // resumes playback
        localPlayer.mediaPlayer.fold({
          //          trackUri.foreach(initMediaPlayer())
        })(p => {
          if (resumeOnAudioFocusGain && !p.isPlaying) {
            p.start()
            resumeOnAudioFocusGain = false
          } else {
            // quits ducking
            p.setVolume(1.0f, 1.0f)
            isDucking = false
          }
        })
      case AudioManager.AUDIOFOCUS_LOSS =>
        // Lost focus for an unbounded amount of time: stops playback and releases media player
        closePlayer()
      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT =>
        // Lost focus for a short time, but we have to stop
        // playback. We don't release the media player because playback
        // is likely to resume
        localPlayer.mediaPlayer.filter(_.isPlaying).foreach(p => {
          p.pause()
          resumeOnAudioFocusGain = true
        })
      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK =>
        // Lost focus for a short time, but it's ok to keep playing
        // at an attenuated level
        localPlayer.mediaPlayer.filter(_.isPlaying).foreach(p => {
          p.setVolume(0.1f, 0.1f)
          isDucking = true
        })
    }
  }

  override def onDestroy(): Unit = {
    closePlayer()
  }

  /**
   * Called when a track has completed playback or is swapped for another;
   * a new mediaplayer is then created if more playback follows.
   */
  def closePlayer(): Unit = {
    // throws IllegalArgumentException if the argument was not registered, haha
    Try(unregisterReceiver(intentReceiver))
    new Notifications(this).cancel()
    wifiLock.foreach(_.release())
    localPlayer.mediaPlayer.foreach(p => {
      if (p.isPlaying) {
        p.stop()
      }
      // reset vs release?
      p.reset()
      p.release()
    })
    localPlayer.closeMediaPlayer()
    val am = audioManager
    am abandonAudioFocus this
    am unregisterMediaButtonEventReceiver mediaButtonBroadcastReceiver
    resumeOnAudioFocusGain = false
    isDucking = false
  }

  /**
   * Used for RPC. But we dont use RPC, can return null.
   *
   * All communication with this service is through intents.
   *
   * @param intent unused
   * @return NULL
   */
  override def onBind(intent: Intent): IBinder = null

  private def showToast(msg: String): Unit = {
    handlerOpt.foreach(_.post(new Runnable {
      def run(): Unit = {
        Toast.makeText(getApplicationContext, msg, Toast.LENGTH_LONG).show()
      }
    }))
  }
}

object MediaService {
  //    val PLAY_ACTION = "org.musicpimp.action.PLAY"
  val RESUME_ACTION = "org.musicpimp.action.RESUME"
  val PAUSE_ACTION = "org.musicpimp.action.PAUSE"
  val RESTART_ACTION = "org.musicpimp.action.RESTART"
  val NEXT_ACTION = "org.musicpimp.action.NEXT"
  val PREV_ACTION = "org.musicpimp.action.PREV"
  val CLOSE_ACTION = "org.musicpimp.action.CLOSE"
  val PLAY_PLAYLIST = "org.musicpimp.action.PLAY_PLAYLIST"

  val TRACK_KEY = "org.musicpimp.keys.TRACK"

  val WIFI_LOCK_TAG = "MusicPimpWifiLock"
}
