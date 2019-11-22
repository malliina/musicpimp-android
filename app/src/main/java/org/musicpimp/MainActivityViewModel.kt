package org.musicpimp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.musicpimp.backend.PimpHttpClient
import org.musicpimp.backend.PimpSocket
import org.musicpimp.backend.SocketDelegate
import org.musicpimp.endpoints.CloudEndpoint
import org.musicpimp.endpoints.Endpoint
import org.musicpimp.endpoints.EndpointManager
import timber.log.Timber
import java.lang.Exception

class MainActivityViewModel(val app: Application) : AndroidViewModel(app) {
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val settings: EndpointManager = EndpointManager.load(app)

    private val times = MutableLiveData<Duration>()
    private val tracks = MutableLiveData<Track>()
    private val states = MutableLiveData<Playstate>().apply {
        value = Playstate.NoMedia
    }
    private val liveDataDelegate = object : SocketDelegate {
        override fun timeUpdated(time: Duration) {
            times.postValue(time)
        }
        override fun trackUpdated(track: Track) {
            tracks.postValue(track)
        }
        override fun playstateUpdated(state: Playstate) {
            states.postValue(state)
        }
    }
    val timeUpdates: LiveData<Duration> = times
    val trackUpdates: LiveData<Track> = tracks
    val stateUpdates: LiveData<Playstate> = states

    var http: PimpHttpClient? = null
    var socket: PimpSocket? = null

    init {
        settings.active()?.let { endpoint ->
            updateBackend(endpoint.creds.authHeader)
            openSocket()
        }
    }

    fun activate(endpoint: Endpoint) {
        settings.saveActive(endpoint.id)
        if (endpoint is CloudEndpoint) {
            updateBackend(endpoint.creds.authHeader)
        }
    }

    private fun updateBackend(header: AuthHeader) {
        http = PimpHttpClient.build(app, header)
        socket = PimpSocket.build(header, liveDataDelegate)
    }

    fun openSocket() {
        uiScope.launch {
            try {
                socket?.connect()
            } catch(e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun closeSocket() {
        try {
            socket?.disconnect()
        } catch(e: Exception) {
            Timber.e(e, "Failed to disconnect.")
        }
    }
//    fun <T> send(message: T, adapter: JsonAdapter<T>) = socket.send(message, adapter)
}
