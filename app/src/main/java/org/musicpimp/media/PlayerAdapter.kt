package org.musicpimp.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.support.v4.media.MediaMetadataCompat
import androidx.media.AudioManagerCompat

/**
 * Abstract player implementation that handles playing music with proper handling of headphones
 * and audio focus.
 */
abstract class PlayerAdapter(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val defaultVolume = 1.0f
    private val duckVolume = 0.2f

    private val noisyIntentFilter =
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var mAudioNoisyReceiverRegistered = false
    private val mAudioFocusHelper = object: AudioManager.OnAudioFocusChangeListener {
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

    abstract fun playFromMedia(metadata: MediaMetadataCompat)
    abstract fun getCurrentMedia(): MediaMetadataCompat?
    abstract fun isPlaying(): Boolean
    abstract fun onPlay()
    abstract fun onPause()
    abstract fun onStop()
    abstract fun seekTo(position: Long)
    abstract fun setVolume(volume: Float)

    fun play() {
        if (mAudioFocusHelper.requestAudioFocus()) {
            registerAudioNoisyReceiver()
            onPlay()
        }
    }

    fun pause() {
        if (!mPlayOnAudioFocus) {
            mAudioFocusHelper.abandonAudioFocus()
        }
        unregisterAudioNoisyReceiver()
        onPause()
    }

    fun stop() {
        mAudioFocusHelper.abandonAudioFocus()
        unregisterAudioNoisyReceiver()
        onStop()
    }

    private fun registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            appContext.registerReceiver(mAudioNoisyReceiver, noisyIntentFilter)
            mAudioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            appContext.unregisterReceiver(mAudioNoisyReceiver)
            mAudioNoisyReceiverRegistered = false
        }
    }
}
