package org.musicpimp.media

import android.content.ComponentName
import android.content.Context
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaControllerCompat.Callback
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import org.musicpimp.Track
import timber.log.Timber

/**
 * Helper class for a MediaBrowser that handles connecting, disconnecting,
 * and basic browsing with simplified callbacks.
 *
 * TODO This class is potentially useless; let's try to get rid of it.
 */
class MediaBrowserHelper(
    val context: Context,
    private val serviceClass: Class<out MediaBrowserServiceCompat>
) {
    private val callbackList = mutableListOf<Callback>()
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback()
    private val mediaControllerCallback = MediaControllerCallback()
    private val mediaBrowserSubscriptionCallback = MediaBrowserSubscriptionCallback()
    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null

    val ctrl: MediaControllerCompat?
        get() = mediaController

    fun reset(track: Track) {
        resetAll(listOf(track))
    }

    private fun resetAll(tracks: List<Track>) {
        clear()
        tracks.forEach { t ->
            ctrl?.addQueueItem(LocalPlaylist.toMedia(t).description)
        }
    }

    private fun clear() {
        // WTF?
        ctrl?.let { c ->
            c.queue?.let { q ->
                q.forEach { item ->
                    c.removeQueueItem(item.description)
                }
            }
        }
    }

    fun onStart() {
        if (mediaBrowser == null) {
            val browser = MediaBrowserCompat(
                context,
                ComponentName(context, serviceClass),
                mediaBrowserConnectionCallback,
                null
            )
            mediaBrowser = browser
            browser.connect()
        }
        Timber.d("onStart: Creating MediaBrowser, and connecting")
    }

    fun onStop() {
        mediaController?.let {
            it.unregisterCallback(mediaControllerCallback)
            mediaController = null
        }
        mediaBrowser?.let { browser ->
            if (browser.isConnected) {
                browser.disconnect()
                mediaBrowser = null
            }
        }
        resetState()
        Timber.d("onStop: Releasing MediaController, Disconnecting from MediaBrowser")
    }

    /**
     * Called after connecting with a {@link MediaBrowserServiceCompat}.
     * <p>
     * Override to perform processing after a connection is established.
     *
     * @param mediaController {@link MediaControllerCompat} associated with the connected
     *                        MediaSession.
     */
    fun onConnected(mediaController: MediaControllerCompat) {
    }

    /**
     * Called after loading a browsable {@link MediaBrowserCompat.MediaItem}
     *
     * @param parentId The media ID of the parent item.
     * @param children List (possibly empty) of child items.
     */
    fun onChildrenLoaded(
        parentId: String,
        children: List<MediaBrowserCompat.MediaItem>
    ) {
    }

    /**
     * Called when the {@link MediaBrowserServiceCompat} connection is lost.
     */
    fun onDisconnected() {
    }

    /**
     * The internal state of the app needs to revert to what it looks like when it started before
     * any connections to the {@link MusicService} happens via the {@link MediaSessionCompat}.
     */
    fun resetState() {
        performOnAllCallbacks(object : CallbackCommand {
            override fun perform(callback: MediaControllerCompat.Callback) {
                callback.onPlaybackStateChanged(null)
            }
        })
    }

    fun transportControls(): MediaControllerCompat.TransportControls? {
        return mediaController?.transportControls
    }

    fun registerCallback(callback: Callback) {
        callbackList.add(callback)

        // Update with the latest metadata/playback state.
        mediaController?.let { ctrl ->
            ctrl.metadata?.let {
                callback.onMetadataChanged(it)
            }
            ctrl.playbackState?.let {
                callback.onPlaybackStateChanged(it)
            }
        }
    }

    private fun performOnAllCallbacks(command: CallbackCommand) {
        callbackList.forEach { callback ->
            command.perform(callback)
        }
    }

    /**
     * Helper for more easily performing operations on all listening clients.
     */
    interface CallbackCommand {
        fun perform(callback: MediaControllerCompat.Callback)
    }

    // Receives callbacks from the MediaBrowser when it has successfully connected to the
    // MediaBrowserService (MusicService).
    inner class MediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {

        // Called as a result of onStart().
        override fun onConnected() {
            try {
                val sessionToken = mediaBrowser?.sessionToken
                sessionToken?.let { token ->
                    // Get a MediaController for the MediaSession.
                    val ctrl = MediaControllerCompat(context, token)
                    mediaController = ctrl
                    Timber.i("Initialized media controller.")
                    ctrl.registerCallback(mediaControllerCallback)
                    // Sync existing MediaSession state to the UI.
                    mediaControllerCallback.onMetadataChanged(ctrl.metadata)
                    mediaControllerCallback.onPlaybackStateChanged(
                        ctrl.playbackState
                    )
                    (this@MediaBrowserHelper).onConnected(ctrl)
                }
            } catch (e: RemoteException) {
                Timber.d(e, "onConnected problem.")
                throw RuntimeException(e)
            }
            mediaBrowser?.let {
                it.subscribe(it.root, mediaBrowserSubscriptionCallback)
            }
        }
    }

    // Receives callbacks from the MediaBrowser when the MediaBrowserService has loaded new media
    // that is ready for playback.
    inner class MediaBrowserSubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaBrowserCompat.MediaItem>
        ) {
            (this@MediaBrowserHelper).onChildrenLoaded(parentId, children)
        }
    }

    // Receives callbacks from the MediaController and updates the UI state,
    // i.e.: Which is the current item, whether it's playing or paused, etc.
    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            performOnAllCallbacks(object : CallbackCommand {
                override fun perform(callback: MediaControllerCompat.Callback) {
                    callback.onMetadataChanged(metadata)
                }
            })
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            performOnAllCallbacks(object : CallbackCommand {
                override fun perform(callback: MediaControllerCompat.Callback) {
                    callback.onPlaybackStateChanged(state)
                }
            })
        }

        // This might happen if the MusicService is killed while the Activity is in the
        // foreground and onStart() has been called (but not onStop()).
        override fun onSessionDestroyed() {
            resetState()
            onPlaybackStateChanged(null)
            (this@MediaBrowserHelper).onDisconnected()
        }
    }
}
