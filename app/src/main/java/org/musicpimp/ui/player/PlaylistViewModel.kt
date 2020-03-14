package org.musicpimp.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.musicpimp.MainActivityViewModel
import org.musicpimp.Track
import org.musicpimp.ui.playlists.TracksViewModel

class PlaylistViewModel(app: Application) : TracksViewModel<Track>(app) {
    override suspend fun load(from: Int, until: Int): List<Track> {
        return emptyList<Track>()
    }
}
