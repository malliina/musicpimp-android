package org.musicpimp.ui.music

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.musicpimp.AuthHeader
import org.musicpimp.Directory
import org.musicpimp.FolderId
import org.musicpimp.backend.PimpHttpClient
import org.musicpimp.endpoints.CloudEndpoint
import org.musicpimp.endpoints.EndpointManager
import timber.log.Timber

class MusicViewModelFactory(val app: Application, val http: PimpHttpClient): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MusicViewModel(app, http) as T
    }
}

class MusicViewModel(val app: Application, val http: PimpHttpClient) : AndroidViewModel(app) {
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
//    private val settings: EndpointManager = EndpointManager.load(app)
//    private val http: PimpHttpClient = PimpHttpClient.build(app, authHeader())

    private val dir = MutableLiveData<Directory>()
    val directory: LiveData<Directory> = dir

    fun loadFolder(id: FolderId) {
        uiScope.launch {
            try {
//                val response = cache.getOrPut(id) { http.folder(id) }
                val response = http.folder(id)
                dir.value = response
                Timber.i("Loaded ${response.folder.path}")
            } catch (e: Exception) {
                val msg =
                    if (id == FolderId.root) "Failed to load root directory."
                    else "Failed to load directory $id."
                Timber.e(e, msg)
            }
        }
    }
}
