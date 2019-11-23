package org.musicpimp.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.musicpimp.MainActivityViewModel

class PlayerViewModelFactory(val app: Application, val main: MainActivityViewModel) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlayerViewModel(app, main) as T
    }
}

class PlayerViewModel(app: Application, val main: MainActivityViewModel) : AndroidViewModel(app) {
    fun onPlayPause() {
        main.playerSocket?.resume()
    }

    fun onNext() {
        main.playerSocket?.next()
    }

    fun onPrevious() {
        main.playerSocket?.prev()
    }
}
