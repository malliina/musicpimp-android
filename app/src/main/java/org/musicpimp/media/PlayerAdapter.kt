package org.musicpimp.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.support.v4.media.MediaMetadataCompat;

/**
 * Abstract player implementation that handles playing music with proper handling of headphones
 * and audio focus.
 */
abstract class PlayerAdapter(context: Context) {
    private val mApplicationContext = context.applicationContext
    private val mAudioManager = mApplicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val MEDIA_VOLUME_DEFAULT = 1.0f
    private val MEDIA_VOLUME_DUCK = 0.2f

    private val AUDIO_NOISY_INTENT_FILTER =
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var mAudioNoisyReceiverRegistered = false

    private val mAudioFocusHelper = object: AudioManager.OnAudioFocusChangeListener {

        fun requestAudioFocus(): Boolean {
            val result = mAudioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        fun abandonAudioFocus() {
            mAudioManager.abandonAudioFocus(this)
        }

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (mPlayOnAudioFocus && !isPlaying()) {
                        play()
                    } else if (isPlaying()) {
                        setVolume(MEDIA_VOLUME_DEFAULT)
                    }
                    mPlayOnAudioFocus = false
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    setVolume(MEDIA_VOLUME_DUCK)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (isPlaying()) {
                        mPlayOnAudioFocus = true
                        pause()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    mAudioManager.abandonAudioFocus(this)
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

    fun registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mApplicationContext.registerReceiver(mAudioNoisyReceiver, AUDIO_NOISY_INTENT_FILTER)
            mAudioNoisyReceiverRegistered = true
        }
    }

    fun unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mApplicationContext.unregisterReceiver(mAudioNoisyReceiver)
            mAudioNoisyReceiverRegistered = false
        }
    }
}
