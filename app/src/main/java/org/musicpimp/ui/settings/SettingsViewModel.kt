package org.musicpimp.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.musicpimp.EndpointId
import org.musicpimp.endpoints.DirectEndpointInput
import org.musicpimp.endpoints.Endpoint
import org.musicpimp.endpoints.EndpointInput
import org.musicpimp.endpoints.EndpointManager

class SettingsViewModel(app: Application) : AndroidViewModel(app), EndpointsDelegate {
    private val settings =
        EndpointManager(app.getSharedPreferences("org.musicpimp.prefs", Context.MODE_PRIVATE))

    private val endpointsData = MutableLiveData<List<Endpoint>>().apply {
        value = settings.fetch().endpoints
    }

    val endpoints: LiveData<List<Endpoint>> = endpointsData

    var editedEndpoint: Endpoint? = null

    override fun onEndpoint(e: Endpoint) {
        editedEndpoint = e
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
