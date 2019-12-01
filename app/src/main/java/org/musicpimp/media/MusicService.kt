package org.musicpimp.media

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import org.musicpimp.PimpApp
import org.musicpimp.TrackId
import timber.log.Timber

class MusicService : MediaBrowserServiceCompat() {
    private val tag = "MusicService"
    private lateinit var session: MediaSessionCompat
    private lateinit var playback: PlayerAdapter
    private lateinit var mediaNotificationManager: MediaNotificationManager
    private lateinit var callback: MediaSessionCallback
    private var serviceInStartedState = false
    private lateinit var library: LocalPlaylist
    private lateinit var listener: MediaPlayerListener

    override fun onCreate() {
        super.onCreate()
        val app = application as PimpApp
        // Create a new MediaSession.
        callback = MediaSessionCallback()
        session = MediaSessionCompat(this, tag).apply {
            setCallback(callback)
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
        }
        sessionToken = session.sessionToken
        library = app.conf.local.playlist
        mediaNotificationManager = MediaNotificationManager(this, library)
        listener = MediaPlayerListener()
        playback = MediaPlayerAdapter(app, listener, library)
        Timber.d(tag, "onCreate: MusicService creating MediaSession, and MediaNotificationManager")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        mediaNotificationManager.onDestroy()
        playback.stop()
        session.release()
        Timber.d(tag, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(library.root(), null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(library.mediaItems().toMutableList())
    }

    // MediaSession Callback: Transport Controls -> MediaPlayerAdapter
    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        private val playlist = mutableListOf<MediaSessionCompat.QueueItem>()
        private var queueIndex = -1
        private var preparedMedia: MediaMetadataCompat? = null

        override fun onAddQueueItem(description: MediaDescriptionCompat) {
            playlist.add(
                MediaSessionCompat.QueueItem(
                    description,
                    description.hashCode().toLong()
                )
            )
            queueIndex = if (queueIndex == -1) 0 else queueIndex
            session.setQueue(playlist)
            Timber.i("Added $description to queue.")
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat) {
            playlist.remove(
                MediaSessionCompat.QueueItem(
                    description,
                    description.hashCode().toLong()
                )
            )
            queueIndex = if (playlist.isEmpty()) -1 else queueIndex
            session.setQueue(playlist)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Timber.i("Media $mediaId")
            mediaId?.let {
                library.metadata(TrackId(it))?.let { meta ->
                    playlist.clear()
                    onAddQueueItem(meta.description)
                    onPlay()
                }
            }
        }

        override fun onPrepare() {
            if (queueIndex < 0 && playlist.isEmpty()) {
                Timber.i("Prepared but nothing to play.")
                // Nothing to play
                return
            }

            val mediaId = playlist[queueIndex].description.mediaId
            preparedMedia = mediaId?.let { library.metadata(TrackId(it)) }
            session.setMetadata(preparedMedia)

            if (!session.isActive) {
                session.isActive = true
            }
        }

        override fun onPlay() {
            Timber.i("onPlay start")
            if (!isReadyToPlay()) {
                // Nothing to play.
                Timber.i("Called onPlay with nothing to play.")
                return
            }
            if (preparedMedia == null) {
                onPrepare()
            }
            preparedMedia?.let {
                Timber.i("Playing from ${it.description}")
                playback.playFromMedia(it)
                Timber.d(tag, "onPlayFromMediaId: MediaSession active")
            }
            Timber.i("onPlay done")
        }

        override fun onPause() {
            playback.pause()
        }

        override fun onStop() {
            playback.stop()
            session.isActive = false
        }

        override fun onSkipToNext() {
            queueIndex = ++queueIndex % playlist.size
            preparedMedia = null
            onPlay()
        }

        override fun onSkipToPrevious() {
            queueIndex = if (queueIndex > 0) queueIndex - 1 else playlist.size - 1
            preparedMedia = null
            onPlay()
        }

        override fun onSeekTo(pos: Long) {
            playback.seekTo(pos)
        }

        private fun isReadyToPlay() = playlist.isNotEmpty()
    }

    // MediaPlayerAdapter Callback: MediaPlayerAdapter state -> MusicService.
    inner class MediaPlayerListener : PlaybackInfoListener() {
        private val serviceManager = ServiceManager()
        private val stateUpdates = MutableLiveData<PlaybackStateCompat>()
        val updates: LiveData<PlaybackStateCompat> = stateUpdates

        override fun onPlaybackStateChange(state: PlaybackStateCompat) {
            // Report the state to the MediaSession.
            session.setPlaybackState(state)

            // Manage the started state of this service.
            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    serviceManager.moveServiceToStartedState(state)
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    serviceManager.updateNotificationForPause(state)
                }
                PlaybackStateCompat.STATE_STOPPED -> {
                    serviceManager.moveServiceOutOfStartedState(state)
                }
            }
            stateUpdates.postValue(state)
        }

        inner class ServiceManager {
            fun moveServiceToStartedState(state: PlaybackStateCompat) {
                playback.getCurrentMedia()?.let { current ->
                    sessionToken?.let { token ->
                        val notification =
                            mediaNotificationManager.getNotification(
                                current, state, token
                            )

                        if (!serviceInStartedState) {
                            ContextCompat.startForegroundService(
                                this@MusicService,
                                Intent(this@MusicService, MusicService::class.java)
                            )
                            serviceInStartedState = true
                        }
                        startForeground(MediaNotificationManager.notificationId, notification)
                    }
                }
            }

            fun updateNotificationForPause(state: PlaybackStateCompat) {
                stopForeground(false)
                playback.getCurrentMedia()?.let { current ->
                    sessionToken?.let { token ->
                        val notification =
                            mediaNotificationManager.getNotification(current, state, token)
                        mediaNotificationManager.mNotificationManager
                            .notify(MediaNotificationManager.notificationId, notification)
                    }

                }

            }

            fun moveServiceOutOfStartedState(state: PlaybackStateCompat) {
                stopForeground(true)
                stopSelf()
                serviceInStartedState = false
            }
        }
    }
}
