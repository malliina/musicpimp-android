package org.musicpimp.media

import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.musicpimp.FolderId
import org.musicpimp.Track
import org.musicpimp.audio.Player
import timber.log.Timber

class LocalPlayer(val browser: MediaBrowserHelper, val playlist: LocalPlaylist): Player {
    companion object {
        val emptyPlaybackState: PlaybackStateCompat = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
            .build()
    }
    private val transport: MediaControllerCompat.TransportControls?
            get() = browser.transportControls()

    override fun play(track: Track) {
        Timber.i("Playing ${track.title} with controls $transport")
        playlist.reset(track)
        browser.reset(track)
        resume()
        Timber.i("Resumed.")
    }

    override fun add(track: Track) {
        playlist.tracks.add(track)
        browser.ctrl?.addQueueItem(LocalPlaylist.toMedia(track).description)
    }

    override fun resume() {
        transport?.let {
            Timber.i("Playing...")
            it.play()
        }
    }

    override fun stop() {
        transport?.stop()
    }

    override fun next() {
        transport?.skipToNext()
    }

    override fun prev() {
        transport?.skipToPrevious()
    }

    override fun skip(idx: Int) {
        transport?.skipToQueueItem(idx.toLong())
    }

    override fun remove(idx: Int) {
        playlist.tracks.removeAt(idx)
    }

    override fun addFolder(folder: FolderId) {
    }

    fun reset(track: Track) {
        browser.ctrl?.queue?.forEach { item -> browser.ctrl?.removeQueueItem(item.description) }
    }
}
