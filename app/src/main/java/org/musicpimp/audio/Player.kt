package org.musicpimp.audio

import org.musicpimp.Duration
import org.musicpimp.FolderId
import org.musicpimp.Track
import org.musicpimp.TrackId

interface Player {
    fun play(track: Track)
    fun add(track: Track)
    fun resume()
    fun stop()
    fun next()
    fun prev()
    fun skip(idx: Int)
    fun remove(idx: Int)
    fun addFolder(folder: FolderId)
    fun seek(to: Duration)
}
