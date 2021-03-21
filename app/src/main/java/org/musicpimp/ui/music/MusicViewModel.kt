package org.musicpimp.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.musicpimp.Directory
import org.musicpimp.FolderId
import org.musicpimp.PimpApp
import org.musicpimp.SingleError
import org.musicpimp.ui.Outcome
import timber.log.Timber

class MusicViewModel(val app: Application) : AndroidViewModel(app) {
    private val conf = (app as PimpApp).components
    private val dir = MutableLiveData<Outcome<Directory>>()
    val directory: LiveData<Outcome<Directory>> = dir

    fun loadFolder(id: FolderId) {
        val http = conf.library
        viewModelScope.launch {
            val name = http.name
            dir.value = Outcome.loading()
            try {
                Timber.i("Loading '$id' from '$name'...")
                val response = http.folder(id)
                dir.value = Outcome.success(response)
                val path = response.folder.path
                val describe = if (id == FolderId.root || id.isBlank()) "Root folder" else path
                Timber.i("Loaded '$describe' from '$name'.")
            } catch (e: Exception) {
                val msg =
                    if (id == FolderId.root) "Failed to load root directory from '$name'."
                    else "Failed to load directory '$id' from '$name'."
                Timber.e(e, msg)
                dir.value = Outcome.error(SingleError.backend(msg))
            }
        }
    }
}
