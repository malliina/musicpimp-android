package org.musicpimp.ui.music

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.musicpimp.Directory
import org.musicpimp.FolderId
import org.musicpimp.MainActivityViewModel
import org.musicpimp.SingleError
import org.musicpimp.backend.PimpHttpClient
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

class MusicViewModelFactory(val app: Application, val main: MainActivityViewModel): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MusicViewModel(app, main) as T
    }
}

class MusicViewModel(val app: Application, private val main: MainActivityViewModel) : AndroidViewModel(app) {
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val dir = MutableLiveData<Outcome<Directory>>()
    val directory: LiveData<Outcome<Directory>> = dir

    fun loadFolder(id: FolderId) {
        main.http?.let { http ->
            uiScope.launch {
                val name = http.name
                dir.value = Outcome.loading()
                try {
                    Timber.i("Loading '$id' from '$name'...")
                    val response = http.folder(id)
                    dir.value = Outcome.success(response)
                    val path = response.folder.path
                    val describe = if (id == FolderId.root) "Root folder" else path
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
