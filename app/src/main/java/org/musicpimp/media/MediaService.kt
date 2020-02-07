package org.musicpimp.media

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.media.AudioManagerCompat
import org.musicpimp.Duration
import timber.log.Timber

/**
 * Abstract player implementation that handles playing music with proper handling of headphones
 * and audio focus.
 */
abstract class MediaService : Service() {
    private val audioManager: AudioManager
        get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val defaultVolume = 1.0f
    private val duckVolume = 0.2f

    protected lateinit var session: MediaSession

    private val noisyIntentFilter =
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var mAudioNoisyReceiverRegistered = false
    private val mAudioFocusHelper = object : AudioManager.OnAudioFocusChangeListener {
        val request = AudioFocusRequest.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(this)
            .build()

        fun requestAudioFocus(): Boolean {
            val result = audioManager.requestAudioFocus(request)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        fun abandonAudioFocus() {
            audioManager.abandonAudioFocusRequest(request)
        }

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (mPlayOnAudioFocus && !isPlaying()) {
                        play()
                    } else if (isPlaying()) {
                        setVolume(defaultVolume)
                    }
                    mPlayOnAudioFocus = false
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    setVolume(duckVolume)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (isPlaying()) {
                        mPlayOnAudioFocus = true
                        pause()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    Timber.i("Lost audio focus.")
                    audioManager.abandonAudioFocusRequest(request)
                    mPlayOnAudioFocus = false
                    stop()
                }
            }
        }
    }
    private val mAudioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                if (isPlaying()) {
                    pause()
                }
            }
        }
    }
    private var mPlayOnAudioFocus = false

    /**
     * setActions(...) defines the playback actions we want to receive events of, which we
     * handle in MediaSession.Callback() { ... }
     */
    protected val stateBuilder: PlaybackState.Builder = PlaybackState.Builder().setActions(
        PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_REWIND or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM or
                PlaybackState.ACTION_PLAY_FROM_URI
    )

    abstract fun isPlaying(): Boolean
    abstract fun onPlay()
    abstract fun onPause()
    abstract fun onStop()
    abstract fun seekTo(position: Duration)
    abstract fun setVolume(volume: Float)
    abstract fun onNext()
    abstract fun onPrev()

    override fun onCreate() {
        super.onCreate()

        /**
         * Required to respond to media buttons (= external hardware buttons, bluetooth headsets,
         * car audio buttons, etc.)
         *
         * Android's official documentation around this sucks, so see
         * https://riptutorial.com/android/example/21618/receiving-and-handling-button-events.

         */
        session = MediaSession(applicationContext, "org.musicpimp.session")
        session.setPlaybackState(stateBuilder.build())
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                play()
            }

            override fun onPause() {
                pause()
            }

            override fun onStop() {
                stop()
            }

            override fun onSkipToNext() {
                onNext()
            }

            override fun onSkipToPrevious() {
                onPrev()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        session.release()
    }

    fun play() {
        if (mAudioFocusHelper.requestAudioFocus()) {
            registerAudioNoisyReceiver()
            session.isActive = true
            onPlay()
        }
    }

    fun pause() {
        if (!mPlayOnAudioFocus) {
            mAudioFocusHelper.abandonAudioFocus()
        }
        unregisterAudioNoisyReceiver()
        session.isActive = false
        onPause()
    }

    fun stop() {
        mAudioFocusHelper.abandonAudioFocus()
        unregisterAudioNoisyReceiver()
        session.isActive = false
        onStop()
    }

    private fun registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            applicationContext.registerReceiver(mAudioNoisyReceiver, noisyIntentFilter)
            mAudioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            applicationContext.unregisterReceiver(mAudioNoisyReceiver)
            mAudioNoisyReceiverRegistered = false
        }
    }
}
