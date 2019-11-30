package org.musicpimp.media

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat

class MusicService : MediaBrowserServiceCompat() {
    private val tag = "MusicService"
    private lateinit var mSession: MediaSessionCompat
    private lateinit var mPlayback: PlayerAdapter
    private lateinit var mMediaNotificationManager: MediaNotificationManager
    private lateinit var mCallback: MediaSessionCallback
    private var mServiceInStartedState = false
    private val library = MusicLibrary()

    override fun onCreate() {
        super.onCreate()
        // Create a new MediaSession.
        mCallback = MediaSessionCallback()
        mSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mCallback)
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
        }
        sessionToken = mSession.sessionToken
        mMediaNotificationManager = MediaNotificationManager(this, library)
        mPlayback = MediaPlayerAdapter(this, MediaPlayerListener(), library)
        Log.d(tag, "onCreate: MusicService creating MediaSession, and MediaNotificationManager")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        mMediaNotificationManager.onDestroy()
        mPlayback.stop()
        mSession.release()
        Log.d(tag, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released")
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
        val mPlaylist = mutableListOf<MediaSessionCompat.QueueItem>()
        private var mQueueIndex = -1
        private var mPreparedMedia: MediaMetadataCompat? = null

        override fun onAddQueueItem(description: MediaDescriptionCompat) {
            mPlaylist.add(
                MediaSessionCompat.QueueItem(
                    description,
                    description.hashCode().toLong()
                )
            )
            mQueueIndex = if (mQueueIndex == -1) 0 else mQueueIndex
            mSession.setQueue(mPlaylist)
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat) {
            mPlaylist.remove(
                MediaSessionCompat.QueueItem(
                    description,
                    description.hashCode().toLong()
                )
            )
            mQueueIndex = if (mPlaylist.isEmpty()) -1 else mQueueIndex
            mSession.setQueue(mPlaylist)
        }

        override fun onPrepare() {
            if (mQueueIndex < 0 && mPlaylist.isEmpty()) {
                // Nothing to play.
                return
            }

            val mediaId = mPlaylist[mQueueIndex].description.mediaId
            mPreparedMedia = library.metadata(this@MusicService, mediaId)
            mSession.setMetadata(mPreparedMedia)

            if (!mSession.isActive()) {
                mSession.setActive(true)
            }
        }

        override fun onPlay() {
            if (!isReadyToPlay()) {
                // Nothing to play.
                return
            }
            if (mPreparedMedia == null) {
                onPrepare()
            }
            mPreparedMedia?.let {
                mPlayback.playFromMedia(it)
                Log.d(tag, "onPlayFromMediaId: MediaSession active")
            }
        }

        override fun onPause() {
            mPlayback.pause()
        }

        override fun onStop() {
            mPlayback.stop()
            mSession.isActive = false
        }

        override fun onSkipToNext() {
            mQueueIndex = (++mQueueIndex % mPlaylist.size)
            mPreparedMedia = null
            onPlay()
        }

        override fun onSkipToPrevious() {
            mQueueIndex = if (mQueueIndex > 0) mQueueIndex - 1 else (mPlaylist.size - 1)
            mPreparedMedia = null
            onPlay()
        }

        override fun onSeekTo(pos: Long) {
            mPlayback.seekTo(pos)
        }

        fun isReadyToPlay() = mPlaylist.isNotEmpty()
    }

    // MediaPlayerAdapter Callback: MediaPlayerAdapter state -> MusicService.
    inner class MediaPlayerListener : PlaybackInfoListener() {
        val mServiceManager = ServiceManager()

        override fun onPlaybackStateChange(state: PlaybackStateCompat) {
            // Report the state to the MediaSession.
            mSession.setPlaybackState(state)

            // Manage the started state of this service.
            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    mServiceManager.moveServiceToStartedState(state)
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    mServiceManager.updateNotificationForPause(state)
                }
                PlaybackStateCompat.STATE_STOPPED -> {
                    mServiceManager.moveServiceOutOfStartedState(state)
                }

            }
        }

        inner class ServiceManager {
            fun moveServiceToStartedState(state: PlaybackStateCompat) {
                mPlayback.getCurrentMedia()?.let { current ->
                    sessionToken?.let { token ->
                        val notification =
                            mMediaNotificationManager.getNotification(
                                current, state, token
                            )

                        if (!mServiceInStartedState) {
                            ContextCompat.startForegroundService(
                                this@MusicService,
                                Intent(this@MusicService, MusicService::class.java)
                            )
                            mServiceInStartedState = true
                        }
                        startForeground(MediaNotificationManager.notificationId, notification)
                    }
                }


            }

            fun updateNotificationForPause(state: PlaybackStateCompat) {
                stopForeground(false)
                mPlayback.getCurrentMedia()?.let { current ->
                    sessionToken?.let { token ->
                        val notification =
                            mMediaNotificationManager.getNotification(current, state, token)
                        mMediaNotificationManager.mNotificationManager
                            .notify(MediaNotificationManager.notificationId, notification)
                    }

                }

            }

            fun moveServiceOutOfStartedState(state: PlaybackStateCompat) {
                stopForeground(true)
                stopSelf()
                mServiceInStartedState = false
            }
        }
    }
}
