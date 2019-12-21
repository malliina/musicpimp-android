package org.musicpimp.media

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

/**
 * Implementation of the [MediaControllerCompat.Callback] methods we're interested in.
 *
 *
 * Here would also be where one could override
 * `onQueueChanged(List<MediaSessionCompat.QueueItem> queue)` to get informed when items
 * are added or removed from the queue. We don't do this here in order to keep the UI
 * simple.
 */
class MediaBrowserListener : MediaControllerCompat.Callback() {
    private val localUpdates = MutableLiveData<PlaybackStateCompat>().apply {
        value = LocalPlayer.emptyPlaybackState
    }
    val updates: LiveData<PlaybackStateCompat> = localUpdates
    private val metadataUpdates = MutableLiveData<MediaMetadataCompat>()
    private val metadata: LiveData<MediaMetadataCompat> = metadataUpdates
    val tracks = Transformations.map(metadata) { LocalPlaylist.fromMedia(it) }

    override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat?) {
        localUpdates.postValue(playbackState ?: LocalPlayer.emptyPlaybackState)
    }

    override fun onMetadataChanged(mediaMetadata: MediaMetadataCompat?) {
        mediaMetadata?.let { metadataUpdates.postValue(it) }
        // Change titles, etc
//        mediaMetadata.description.mediaId
//        if (mediaMetadata == null) {
//            return
//        }
//        mTitleTextView.setText(
//            mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
//        )
//        mArtistTextView.setText(
//            mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
//        )
//        mAlbumArt.setImageBitmap(
//            MusicLibrary.getAlbumBitmap(
//                this@MainActivity,
//                mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
//            )
//        )
    }

    override fun onSessionDestroyed() {
        super.onSessionDestroyed()
    }

    override fun onQueueChanged(queue: List<MediaSessionCompat.QueueItem>) {
        super.onQueueChanged(queue)
    }
}