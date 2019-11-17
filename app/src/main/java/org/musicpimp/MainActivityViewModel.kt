package org.musicpimp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.musicpimp.backend.PimpHttpClient
import org.musicpimp.backend.PimpSocket
import org.musicpimp.backend.SocketDelegate
import org.musicpimp.endpoints.CloudEndpoint
import org.musicpimp.endpoints.EndpointManager

class MainActivityViewModel(val app: Application) : AndroidViewModel(app) {
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val settings: EndpointManager = EndpointManager.load(app)
    val http: PimpHttpClient = PimpHttpClient.build(app, authHeader())

    private val times = MutableLiveData<Duration>()
    private val tracks = MutableLiveData<Track>()
    private val states = MutableLiveData<Playstate>()
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
    val socketClient = PimpSocket.build(authHeader(), liveDataDelegate)
    val timeUpdates: LiveData<Duration> = times
    val trackUpdates: LiveData<Track> = tracks
    val stateUpdates: LiveData<Playstate> = states

    init {
        openSocket()
    }

    private fun authHeader(): AuthHeader {
        val e = settings.fetch().endpoints.firstOrNull()
        return if (e != null && e is CloudEndpoint) e.creds.authHeader else AuthHeader("")
    }

    fun openSocket() {
        uiScope.launch {
            socketClient.connect()
        }
    }
//    fun <T> send(message: T, adapter: JsonAdapter<T>) = socketClient.send(message, adapter)
}
