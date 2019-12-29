package org.musicpimp.media

import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.musicpimp.PimpApp
import org.musicpimp.Track
import org.musicpimp.TrackId
import org.musicpimp.backend.HttpClient
import timber.log.Timber
import java.lang.Exception

/**
 * Exposes the functionality of the {@link MediaPlayer} and implements the {@link PlayerAdapter}
 * so that {@link MainActivity} can control music playback.
 */
class MediaPlayerAdapter(
    val app: PimpApp,
    private val listener: PlaybackInfoListener,
    private val library: LocalPlaylist
) : PlayerAdapter(app.applicationContext) {
    private val appContext = app.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: Track? = null
    private var currentMedia: MediaMetadataCompat? = null
    private var state: Int = 0
    private var currentMediaPlayedToCompletion: Boolean = false

    // Work-around for a MediaPlayer bug related to the behavior of MediaPlayer.seekTo()
    // while not playing.
    private var seekWhileNotPlaying: Long = -1

    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {@link MainActivity} the {@link MediaPlayer} is
     * released. Then in the onStart() of the {@link MainActivity} a new {@link MediaPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    private fun initializeMediaPlayer() {
        Timber.i("Initializing media player...")
        if (mediaPlayer == null) {
            val p = MediaPlayer()
            p.setOnCompletionListener {
                listener.onPlaybackCompleted()
                setNewState(PlaybackStateCompat.STATE_PAUSED)
            }
            p.setOnPreparedListener { player ->
                if (player == p) {
                    play()
                }
            }
            mediaPlayer = p
        }
    }

    // Implements PlaybackControl.
    override fun playFromMedia(metadata: MediaMetadataCompat) {
        Timber.i("Playing media ID ${metadata.description.mediaId ?: "unknown id"}")
        currentMedia = metadata
        metadata.description.mediaId?.let { id ->
            library.track(TrackId(id))?.let { track ->
                Timber.i("Playing ${track.title} by ${track.artist}...")
                playTrack(track)
            }
        }
    }

    override fun getCurrentMedia(): MediaMetadataCompat? {
        return currentMedia
    }

    private fun playTrack(track: Track) {
        val current = currentTrack
        val mediaChanged = current == null || current.id != track.id || currentMediaPlayedToCompletion
        if (currentMediaPlayedToCompletion) {
            // Last audio file was played to completion, the resourceId hasn't changed, but the
            // player was released, so force a reload of the media for playback.
            currentMediaPlayedToCompletion = false
        }
        Timber.i("Playing track ${track.title}. Was ${current?.title ?: "no track"}. Changed: $mediaChanged.")
        if (!mediaChanged) {
            if (!isPlaying()) {
                play()
            }
            return
        } else {
            release()
        }
        currentTrack = track
        initializeMediaPlayer()
        try {
            val auth = app.conf.authHeader?.value ?: ""
            Timber.i("Data source '${track.url}' with auth '$auth'.")
            mediaPlayer?.setDataSource(
                appContext,
                Uri.parse(track.url.url),
                mapOf(HttpClient.Authorization to auth)
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to open URL: ${track.url}", e)
        }
        try {
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            throw RuntimeException("Failed to open URL: ${track.url}", e)
        }
        play()
    }

    override fun onStop() {
        // Regardless of whether or not the MediaPlayer has been created / started, the state must
        // be updated, so that MediaNotificationManager can take down the notification.
        setNewState(PlaybackStateCompat.STATE_STOPPED)
        release()
    }

    private fun release() {
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer != null && mediaPlayer?.isPlaying() ?: false
    }

    override fun onPlay() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                Timber.i("onPlay from ${it.currentPosition}...")
                setNewState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    override fun onPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                setNewState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    // This is the main reducer for the player state machine.
    private fun setNewState(newPlayerState: Int) {
        state = newPlayerState
        // Whether playback goes to completion, or whether it is stopped, the
        // mCurrentMediaPlayedToCompletion is set to true.
        if (state == PlaybackStateCompat.STATE_STOPPED) {
            currentMediaPlayedToCompletion = true
        }
        // Work around for MediaPlayer.getCurrentPosition() when it changes while not playing.
        var reportPosition = 0L
        if (seekWhileNotPlaying >= 0) {
            reportPosition = seekWhileNotPlaying
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                seekWhileNotPlaying = -1
            }
        } else {
            reportPosition = mediaPlayer?.currentPosition?.toLong() ?: 0L
        }
        val stateBuilder = PlaybackStateCompat.Builder()
        stateBuilder.setActions(availableActions())
        stateBuilder.setState(
            state,
            reportPosition,
            1.0f,
            SystemClock.elapsedRealtime()
        )
        listener.onPlaybackStateChange(stateBuilder.build())
        Timber.i("Updated state at position $reportPosition")
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
        val additional = when (state) {
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
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                seekWhileNotPlaying = position
            }
            Timber.i("Attempting to seek to $position...")
            player.seekTo(position.toInt())

            // Set the state (to the current state) because the position changed and should
            // be reported to clients.
            setNewState(state)
        }
    }

    override fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }
}
