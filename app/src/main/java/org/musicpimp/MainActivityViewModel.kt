package org.musicpimp

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.musicpimp.audio.Player
import org.musicpimp.backend.PimpLibrary
import org.musicpimp.backend.PimpSocket
import org.musicpimp.backend.PlayerDelegate
import org.musicpimp.endpoints.CloudEndpoint
import org.musicpimp.endpoints.Endpoint
import org.musicpimp.endpoints.EndpointManager
import org.musicpimp.media.LocalPlayer
import timber.log.Timber

class MainActivityViewModel(val app: Application) : AndroidViewModel(app) {
    val components = (app as PimpApp).components
    private val localPlayer: LocalPlayer get() = components.localPlayer
    val player: Player get() = components.player
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val settings: EndpointManager = EndpointManager.load(app)
    private val times = MutableLiveData<Duration>()
    private val tracks = MutableLiveData<Track>()
    private val states = MutableLiveData<Playstate>().apply {
        value = Playstate.NoMedia
    }
    private val playlist = MutableLiveData<List<Track>>()
    private val index = MutableLiveData<Int?>()

    private val liveDataDelegate = object : PlayerDelegate {
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
            Timber.i("Status with ${status.playlist.size} tracks")
            playlist.postValue(status.playlist)
            index.postValue(status.index)
        }

        override fun playlistUpdated(list: List<Track>) {
            Timber.i("Updated with ${list.size} tracks")
            playlist.postValue(list)
        }

        override fun indexUpdated(idx: Int) {
            index.postValue(idx)
        }
    }
    val timeUpdates: LiveData<Duration> = times
    val trackUpdates: LiveData<Track> = tracks
    val stateUpdates: LiveData<Playstate> = states
    val playlistUpdates: LiveData<List<Track>> = playlist
    val indexUpdates: LiveData<Int?> = index

    // workaround since listener.updates.value does not return the latest value
    private var latestState: Playstate = Playstate.NoMedia
    private val stateObserver = Observer<Playstate> { state ->
        latestState = state
        states.postValue(state)
    }
    private val trackObserver = Observer<Track> { track ->
        tracks.postValue(track)
    }
    private val playlistObserver = Observer<List<Track>> { ts ->
        playlist.postValue(ts)
    }
    private val indexObserver = Observer<Int?> { idx ->
        index.postValue(idx)
    }
    private val positionObserver = Observer<Duration> { time ->
        times.postValue(time)
    }

    init {
        val src = settings.activeSource()
        if (src is CloudEndpoint) {
            setupSource(src)
        }
        setupPlayer(settings.activePlayer(), connect = false)
        localPlayer.tracks.observeForever(trackObserver)
        localPlayer.index.observeForever(indexObserver)
        localPlayer.states.observeForever(stateObserver)
        localPlayer.list.observeForever(playlistObserver)
        localPlayer.position.observeForever(positionObserver)
    }

    override fun onCleared() {
        super.onCleared()
        localPlayer.tracks.removeObserver(trackObserver)
        localPlayer.index.removeObserver(indexObserver)
        localPlayer.states.removeObserver(stateObserver)
        localPlayer.list.removeObserver(playlistObserver)
        localPlayer.position.removeObserver(positionObserver)
    }

    fun activatePlayer(player: Endpoint) {
        settings.saveActivePlayer(player.id)
        setupPlayer(player, connect = true)
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

    private fun updateSource(header: HeaderValue, name: String) {
        components.library = PimpLibrary.build(app, header, name)
        components.authHeader = header
        Timber.i("Updated backend to '$name'.")
    }

    private fun setupPlayer(e: Endpoint, connect: Boolean) {
        closeSocket()
        if (e is CloudEndpoint) {
            val socket = PimpSocket.build(e.creds.authHeader, liveDataDelegate)
            components.playerSocket = socket
            components.player = socket.player
            if (connect)
                openSocket()
        } else {
            components.playerSocket = null
            components.player = localPlayer
            // ?
            localPlayer.list.value
            playlist.postValue(localPlayer.list.value)
        }
    }

    fun openSocket() {
        uiScope.launch {
            try {
                components.playerSocket?.connect()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun closeSocket() {
        try {
            components.playerSocket?.disconnect()
        } catch (e: Exception) {
            Timber.e(e, "Failed to disconnect.")
        }
    }

    fun skip(toIndex: Int) {
        player.skip(toIndex)
    }

    fun remove(idx: Int) {
        player.remove(idx)
    }

    fun play(track: Track) {
        player.play(track)
    }

    fun playAll(tracks: List<Track>) {
        tracks.firstOrNull()?.let {
            player.play(it)
        }
        player.addAll(tracks.drop(1))
    }

    fun add(id: Track) {
        player.add(id)
    }

    fun playFolder(id: FolderId) {
        withAll(id) { all ->
            all.firstOrNull()?.let {
                player.play(it)
            }
            player.addAll(all.drop(1))
        }
    }

    fun addFolder(id: FolderId) {
        withAll(id) { player.addAll(it) }
    }

    private fun withAll(id: FolderId, code: (ts: List<Track>) -> Unit) {
        viewModelScope.launch {
            code(components.library.tracksRecursively(id))
        }
    }

    fun resume() {
        player.resume()
    }

    fun pause() {
        player.stop()
    }

    fun next() {
        player.next()
    }

    fun previous() {
        player.prev()
    }

    fun seek(to: Duration) {
        player.seek(to)
    }
}
