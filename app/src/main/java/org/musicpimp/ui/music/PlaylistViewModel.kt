package org.musicpimp.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.musicpimp.MainActivityViewModel

class PlaylistViewModelFactory(val app: Application, val main: MainActivityViewModel) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlaylistViewModel(app, main) as T
    }
}

class PlaylistViewModel(val app: Application, private val main: MainActivityViewModel) :
    AndroidViewModel(app) {


}
