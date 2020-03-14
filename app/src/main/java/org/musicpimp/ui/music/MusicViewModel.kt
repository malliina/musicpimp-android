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
import timber.log.Timber

enum class Status {
    Success,
    Error,
    Loading
}

data class Outcome<out T>(val status: Status, val data: T?, val error: SingleError?) {
    companion object {
        fun <T> success(t: T): Outcome<T> = Outcome(Status.Success, t, null)
        fun error(err: SingleError): Outcome<Nothing> = Outcome(Status.Error, null, err)
        fun loading(): Outcome<Nothing> = Outcome(Status.Loading, null, null)
    }
}

class MusicViewModel(val app: Application) : AndroidViewModel(app) {
    private val conf = (app as PimpApp).components
    private val dir = MutableLiveData<Outcome<Directory>>()
    val directory: LiveData<Outcome<Directory>> = dir

    fun loadFolder(id: FolderId) {
        conf.library?.let { http ->
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
}
