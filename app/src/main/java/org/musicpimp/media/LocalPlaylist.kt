package org.musicpimp.media

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import org.musicpimp.*
import timber.log.Timber
import kotlin.time.milliseconds

class LocalPlaylist {
    val tracks: MutableList<Track> = mutableListOf()

    companion object {
        private const val path = "org.musicpimp.metadata.path"
        private const val size = "org.musicpimp.metadata.size"
        private const val url = "org.musicpimp.metadata.url"

        fun toMedia(track: Track): MediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id.value)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album.value)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist.value)
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration.toMillis().toLong())
            putString(path, track.path)
            putLong(size, track.size.bytes)
            putString(url, track.url.url)
//        putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
//        putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, getAlbumArtUri(albumArtResName))
//        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, getAlbumArtUri(albumArtResName))
        }.build()

        fun fromMedia(media: MediaMetadataCompat): Track = Track(
            TrackId(media.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)),
            media.getString(MediaMetadataCompat.METADATA_KEY_TITLE),
            Album(media.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)),
            Artist(media.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)),
            media.getString(path),
            media.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).millis,
            media.getLong(size).bytes,
            FullUrl.build(media.getString(url))!!
        )
    }

    fun root(): String {
        Timber.i("Root...")
        return "root"
    }

    fun mediaItems(): List<MediaBrowserCompat.MediaItem> {
        Timber.i("Items...")
        return emptyList()
    }

    fun track(id: TrackId): Track? {
        Timber.i("Track of $id...")
        return tracks.find { it.id == id }
    }

    fun albumCover(mediaId: TrackId, service: MusicService): Bitmap? {
        Timber.i("Cover of $mediaId...")
        return null
    }

    fun metadata(mediaId: TrackId): MediaMetadataCompat? {
        Timber.i("Metadata of $mediaId...")
        return track(mediaId)?.let { toMedia(it) }
    }

    fun reset(track: Track) {
        resetAll(listOf(track))
    }

    fun resetAll(ts: List<Track>) {
        tracks.clear()
        tracks.addAll(ts)
    }
}
