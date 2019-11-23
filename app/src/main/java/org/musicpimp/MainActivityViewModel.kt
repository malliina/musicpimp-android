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
        override fun onStatus(status: StatusMessage) {
            if (status.state != Playstate.NoMedia) {
                tracks.postValue(status.track)
                states.postValue(status.state)
                times.postValue(status.position)
            }
        }
    }
    val timeUpdates: LiveData<Duration> = times
    val trackUpdates: LiveData<Track> = tracks
    val stateUpdates: LiveData<Playstate> = states

    var http: PimpHttpClient? = null
    var playerSocket: PimpSocket? = null

    init {
        settings.activeSource()?.let { source ->
            setupSource(source)
        }
        settings.activePlayer()?.let { player ->
            setupPlayer(player)
        }
    }

    fun activatePlayer(player: Endpoint) {
        settings.saveActivePlayer(player.id)
        if (player is CloudEndpoint) {
            setupPlayer(player)
        }
    }

    fun activateSource(source: Endpoint) {
        settings.saveActiveSource(source.id)
        if (source is CloudEndpoint) {
            setupSource(source)
        }
    }

    private fun setupSource(e: CloudEndpoint) {
        updateSource(e.creds.authHeader, e.creds.server.value)
    }

    private fun updateSource(header: AuthHeader, name: String) {
        http = PimpHttpClient.build(app, header, name)
        Timber.i("Updated backend to '$name'.")
    }

    private fun setupPlayer(e: CloudEndpoint) {
        closeSocket()
        playerSocket = PimpSocket.build(e.creds.authHeader, liveDataDelegate)
        openSocket()
    }

    fun openSocket() {
        uiScope.launch {
            try {
                playerSocket?.connect()
            } catch(e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun closeSocket() {
        try {
            playerSocket?.disconnect()
        } catch(e: Exception) {
            Timber.e(e, "Failed to disconnect.")
        }
    }
//    fun <T> send(message: T, adapter: JsonAdapter<T>) = playerSocket.send(message, adapter)
}
