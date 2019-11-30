package org.musicpimp.media

import android.content.Context
import android.media.MediaPlayer
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import java.lang.Exception

/**
 * Exposes the functionality of the {@link MediaPlayer} and implements the {@link PlayerAdapter}
 * so that {@link MainActivity} can control music playback.
 */
class MediaPlayerAdapter(val context: Context, val listener: PlaybackInfoListener, val library: MusicLibrary) :
    PlayerAdapter(context) {
    val mContext = context.applicationContext
    var mMediaPlayer: MediaPlayer? = null
    var mFilename: String? = null
    var mCurrentMedia: MediaMetadataCompat? = null
    var mState: Int = 0
    var mCurrentMediaPlayedToCompletion: Boolean = false

    // Work-around for a MediaPlayer bug related to the behavior of MediaPlayer.seekTo()
    // while not playing.
    var mSeekWhileNotPlaying: Long = -1

    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {@link MainActivity} the {@link MediaPlayer} is
     * released. Then in the onStart() of the {@link MainActivity} a new {@link MediaPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    fun initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            val p = MediaPlayer()
            p.setOnCompletionListener {
                listener.onPlaybackCompleted()
                setNewState(PlaybackStateCompat.STATE_PAUSED)
            }
            mMediaPlayer = p
        }
    }

    // Implements PlaybackControl.
    override fun playFromMedia(metadata: MediaMetadataCompat) {
        mCurrentMedia = metadata
        val mediaId = metadata.description.mediaId
        playFile(library.filename(mediaId))
    }

    override fun getCurrentMedia(): MediaMetadataCompat? {
        return mCurrentMedia
    }

    fun playFile(filename: String) {
        var mediaChanged = mFilename == null || filename != mFilename
        if (mCurrentMediaPlayedToCompletion) {
            // Last audio file was played to completion, the resourceId hasn't changed, but the
            // player was released, so force a reload of the media file for playback.
            mediaChanged = true
            mCurrentMediaPlayedToCompletion = false
        }
        if (!mediaChanged) {
            if (!isPlaying()) {
                play()
            }
            return
        } else {
            release()
        }

        mFilename = filename

        initializeMediaPlayer()
        try {
            val assetFileDescriptor = mContext.assets.openFd(filename)
            mMediaPlayer?.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to open file: $filename", e)
        }
        try {
            mMediaPlayer?.prepare()
        } catch (e: Exception) {
            throw RuntimeException("Failed to open file: $filename", e)
        }
        play()
    }

    override fun onStop() {
        // Regardless of whether or not the MediaPlayer has been created / started, the state must
        // be updated, so that MediaNotificationManager can take down the notification.
        setNewState(PlaybackStateCompat.STATE_STOPPED)
        release()
    }

    fun release() {
        if (mMediaPlayer != null) {
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }

    override fun isPlaying(): Boolean {
        return mMediaPlayer != null && mMediaPlayer?.isPlaying() ?: false
    }

    override fun onPlay() {
        mMediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                setNewState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    override fun onPause() {
        mMediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                setNewState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    // This is the main reducer for the player state machine.
    fun setNewState(newPlayerState: Int) {
        mState = newPlayerState

        // Whether playback goes to completion, or whether it is stopped, the
        // mCurrentMediaPlayedToCompletion is set to true.
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            mCurrentMediaPlayedToCompletion = true
        }

        // Work around for MediaPlayer.getCurrentPosition() when it changes while not playing.
        var reportPosition = 0L
        if (mSeekWhileNotPlaying >= 0) {
            reportPosition = mSeekWhileNotPlaying;

            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                mSeekWhileNotPlaying = -1
            }
        } else {
            reportPosition = mMediaPlayer?.let { it.currentPosition.toLong() } ?: 0L
        }

        val stateBuilder = PlaybackStateCompat.Builder()
        stateBuilder.setActions(availableActions())
        stateBuilder.setState(
            mState,
            reportPosition,
            1.0f,
            SystemClock.elapsedRealtime()
        )
        listener.onPlaybackStateChange(stateBuilder.build())
    }

    /**
     * Set the current capabilities available on this session. Note: If a capability is not
     * listed in the bitmask of capabilities then the MediaSession will not handle it. For
     * example, if you don't want ACTION_STOP to be handled by the MediaSession, then don't
     * included it in the bitmask that's returned.
     */
    private fun availableActions(): Long {
        val actions =
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        val additional =  when (mState) {
            PlaybackStateCompat.STATE_STOPPED -> {
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
            }
            PlaybackStateCompat.STATE_PLAYING -> {
                PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_STOP
            }
            else -> {
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_PAUSE
            }
        }
        return actions or additional
    }

    override fun seekTo(position: Long) {
        mMediaPlayer?.let { player ->
            if (!player.isPlaying()) {
                mSeekWhileNotPlaying = position
            }
            player.seekTo(position.toInt())

            // Set the state (to the current state) because the position changed and should
            // be reported to clients.
            setNewState(mState)
        }
    }

    override fun setVolume(volume: Float) {
        mMediaPlayer?.setVolume(volume, volume)
    }
}
