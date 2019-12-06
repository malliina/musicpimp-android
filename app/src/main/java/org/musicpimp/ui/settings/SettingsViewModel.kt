package org.musicpimp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.musicpimp.EndpointId
import org.musicpimp.endpoints.Endpoint
import org.musicpimp.endpoints.EndpointManager
import org.musicpimp.endpoints.LocalEndpoint

class SettingsViewModel(app: Application) : AndroidViewModel(app), EndpointsDelegate {
    private val settings = EndpointManager.load(app)

    private val endpointsData = MutableLiveData<List<Endpoint>>().apply {
        value = settings.fetch().endpoints
    }
    private val playback = MutableLiveData<Endpoint>().apply {
        value = settings.activePlayer()
    }
    private val source = MutableLiveData<Endpoint>().apply {
        value = settings.activeSource()
    }

    val endpoints: LiveData<List<Endpoint>> = endpointsData
    val playbackDevice: LiveData<Endpoint> = playback
    val musicSource: LiveData<Endpoint> = source

    var editedEndpoint: Endpoint? = null

    override fun onEndpoint(e: Endpoint) {
        editedEndpoint = e
    }

    fun onPlayback(e: Endpoint) {
        playback.postValue(e)
    }

    fun onSource(e: Endpoint) {
        source.postValue(e)
    }

    fun save(e: Endpoint) {
        settings.save(e)
        updateEndpoints()
    }

    fun removeIfExists() {
        editedEndpoint?.let {
            remove(it.id)
        }
    }

    private fun remove(id: EndpointId) {
        settings.remove(id)
        updateEndpoints()
    }

    private fun updateEndpoints() {
        endpointsData.postValue(settings.fetch().endpoints)
    }
}
