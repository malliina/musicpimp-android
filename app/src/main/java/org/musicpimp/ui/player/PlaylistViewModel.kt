package org.musicpimp.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.musicpimp.MainActivityViewModel
import org.musicpimp.Track
import org.musicpimp.ui.playlists.TracksViewModel

class PlaylistViewModelFactory(val app: Application, val main: MainActivityViewModel) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlaylistViewModel(app, main) as T
    }
}

class PlaylistViewModel(app: Application, private val main: MainActivityViewModel) :
    TracksViewModel<Track>(app) {
    override suspend fun load(from: Int, until: Int): List<Track> {
        return emptyList<Track>()
    }
}
