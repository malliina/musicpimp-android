package org.musicpimp.media

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

/**
 * Implementation of the [MediaControllerCompat.Callback] methods we're interested in.
 *
 *
 * Here would also be where one could override
 * `onQueueChanged(List<MediaSessionCompat.QueueItem> queue)` to get informed when items
 * are added or removed from the queue. We don't do this here in order to keep the UI
 * simple.
 */
private class MediaBrowserListener : MediaControllerCompat.Callback() {
    override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat?) {
//        mIsPlaying = playbackState != null &&
//                playbackState.state == PlaybackStateCompat.STATE_PLAYING
//        mMediaControlsImage.setPressed(mIsPlaying)
    }

    override fun onMetadataChanged(mediaMetadata: MediaMetadataCompat?) {
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
