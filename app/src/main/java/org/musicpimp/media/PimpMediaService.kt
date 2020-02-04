package org.musicpimp.media

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import org.musicpimp.*
import org.musicpimp.backend.HttpClient
import timber.log.Timber


/** A background audio player. An implementation of:
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
class PimpMediaService : MediaService() {
    private var mediaPlayer: MediaPlayer? = null
    private val app: PimpApp get() = application as PimpApp
    private val player: SimplePlayer
        get() = app.components.localPlayer

    // Work-around for a MediaPlayer bug related to the behavior of MediaPlayer.seekTo()
    // while not playing.
    private var seekWhileNotPlaying: Long = -1

    private val handler = Handler(Looper.getMainLooper())
    private val pollInterval = Duration(0.8)
    private var updatePosition = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { i ->
            when (i.action) {
                RESTART_ACTION -> player.playerTrack?.let { playTrack(it) }
                PLAY_PLAYLIST -> player.playerTrack?.let { playTrack(it) }
                NEXT_ACTION -> player.toNext()?.let { playTrack(it) }
                PREV_ACTION -> player.toPrev()?.let { playTrack(it) }
                RESUME_ACTION -> play()
                PAUSE_ACTION -> pause()
                CLOSE_ACTION -> pause()
                SEEK_ACTION -> i.getParcelableExtra<Duration>(POSITION_EXTRA)?.let { seekTo(it) }
                else -> {
                    Timber.w("Unknown intent action: '${i.action}'.")
                }
            }
        }
        /**
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
        return START_STICKY
    }

    private fun playTrack(track: Track) {
        release()
        val mp = initialize()
        try {
            val auth = app.components.authHeader?.value ?: ""
            Timber.i("Data source '${track.url}' with auth '$auth'.")
            mp.setDataSource(
                app.applicationContext,
                Uri.parse(track.url.url),
                mapOf(HttpClient.Authorization to auth)
            )
            player.onTrack(track)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open URL '${track.url}'.")
        }
        try {
            mp.prepareAsync()
        } catch (e: Exception) {
            Timber.e(e, "Failed to prepare URL '${track.url}'.")
        }
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer != null && mediaPlayer?.isPlaying ?: false
    }

    override fun onPlay() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                Timber.i("onPlay from ${it.currentPosition}...")
                onState(Playstate.Playing)
            }
        }
    }

    override fun onPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                onState(Playstate.Paused)
            }
        }
    }

    override fun onStop() {
        release()
        onState(Playstate.Stopped)
    }

    override fun seekTo(position: Duration) {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                seekWhileNotPlaying = position.toMillis().toLong()
            }
            Timber.i("Attempting to seek to $position...")
            player.seekTo(position.toMillis().toInt())
        }
    }

    override fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    private fun onState(state: Playstate) {
        player.onState(state)
        updatePosition = state == Playstate.Playing
        if (updatePosition) {
            checkPlaybackPosition()
        }
    }

    private fun initialize(): MediaPlayer {
        Timber.i("Initializing media player...")
        val mp = mediaPlayer
        if (mp == null) {
            val p = MediaPlayer()
            p.setOnCompletionListener {
                val next = player.toNext()
                if (next != null) {
                    playTrack(next)
                } else {
                    release()
                    onState(Playstate.Stopped)
                }
            }
            p.setOnPreparedListener { player ->
                if (player == p) {
                    play()
                }
            }
            p.setOnErrorListener { _, what, extra ->
                Timber.e("Player error $what with $extra.")
                false
            }
            p.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            mediaPlayer = p
            return p
        } else {
            return mp
        }
    }

    private fun release() {
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
            Timber.i("Released media player.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkPlaybackPosition(): Boolean = handler.postDelayed({
        if (mediaPlayer?.isPlaying == true) {
            try {
                mediaPlayer?.currentPosition?.millis?.let {
                    player.onPosition(it)
                }
            } catch (ise: IllegalStateException) {
                Timber.d("Unable to check position.")
            }
        }
        if (updatePosition)
            checkPlaybackPosition()
    }, pollInterval.toMillis().toLong())


    companion object {
        val CLOSE_ACTION = "org.musicpimp.action.CLOSE"
        val NEXT_ACTION = "org.musicpimp.action.NEXT"
        val PAUSE_ACTION = "org.musicpimp.action.PAUSE"
        val PLAY_PLAYLIST = "org.musicpimp.action.PLAY_PLAYLIST"
        val PREV_ACTION = "org.musicpimp.action.PREV"
        val RESTART_ACTION = "org.musicpimp.action.RESTART"
        val RESUME_ACTION = "org.musicpimp.action.RESUME"
        val SEEK_ACTION = "org.musicpimp.action.SEEK"

        val POSITION_EXTRA = "org.musicpimp.action.POSITION"
    }
}
