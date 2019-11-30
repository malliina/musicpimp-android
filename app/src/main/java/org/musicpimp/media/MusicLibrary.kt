package org.musicpimp.media

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat

class MusicLibrary {
    fun root(): String = ""
    fun mediaItems(): List<MediaBrowserCompat.MediaItem> = emptyList()
    fun filename(id: String?) = ""
    fun albumCover(media: String?, service: MusicService): Bitmap? = null
    fun metadata(context: Context?, mediaId: String?): MediaMetadataCompat? = null
}
