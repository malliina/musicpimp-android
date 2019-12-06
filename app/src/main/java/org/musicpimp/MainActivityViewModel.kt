package org.musicpimp

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
import org.musicpimp.media.LocalPlayer
import org.musicpimp.media.MediaBrowserListener
import timber.log.Timber

class MainActivityViewModel(val app: Application) : AndroidViewModel(app) {
    val conf = (app as PimpApp).conf
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val settings: EndpointManager = EndpointManager.load(app)

    private val times = MutableLiveData<Duration>()
    private val tracks = MutableLiveData<Track>()
    private val states = MutableLiveData<Playstate>().apply {
        value = Playstate.NoMedia
    }
    private val playlist = MutableLiveData<List<Track>>()
    private val index = MutableLiveData<Int>()

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
            playlist.postValue(status.playlist)
            index.postValue(status.index)
        }

        override fun playlistUpdated(list: List<Track>) {
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
    val indexUpdates: LiveData<Int> = index

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())
    private val pollInterval = Duration(0.8)

    private val listener = MediaBrowserListener()
    // workaround since listener.updates.value does not return the latest value
    private var latestState: PlaybackStateCompat = LocalPlayer.emptyPlaybackState
    private val stateObserver = Observer<PlaybackStateCompat> { state ->
        latestState = state
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        checkPlaybackPosition()
        updatePosition = isPlaying
        states.postValue(toState(state))
    }

    init {
        val src = settings.activeSource()
        if (src is CloudEndpoint) {
            setupSource(src)
        }
        setupPlayer(settings.activePlayer())
        val listener = MediaBrowserListener()
        conf.local.browser.registerCallback(listener)
        listener.updates.observeForever(stateObserver)
    }

    override fun onCleared() {
        super.onCleared()
        listener.updates.removeObserver(stateObserver)
        updatePosition = false
    }

    private fun toState(state: PlaybackStateCompat): Playstate = when (state.state) {
        PlaybackStateCompat.STATE_PLAYING -> Playstate.Playing
        PlaybackStateCompat.STATE_STOPPED -> Playstate.Stopped
        PlaybackStateCompat.STATE_PAUSED -> Playstate.Paused
        PlaybackStateCompat.STATE_NONE -> Playstate.NoMedia
        else -> Playstate.Other
    }

    fun activatePlayer(player: Endpoint) {
        settings.saveActivePlayer(player.id)
        setupPlayer(player)
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
        conf.http = PimpHttpClient.build(app, header, name)
        Timber.i("Updated backend to '$name'.")
    }

    private fun setupPlayer(e: Endpoint) {
        closeSocket()
        if (e is CloudEndpoint) {
            val socket = PimpSocket.build(e.creds.authHeader, liveDataDelegate)
            conf.playerSocket = socket
            conf.player = socket.player
            openSocket()
        } else {
            conf.playerSocket = null
            conf.player = conf.local
            playlist.postValue(conf.local.playlist.tracks)
        }
    }

    fun openSocket() {
        uiScope.launch {
            try {
                conf.playerSocket?.connect()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun closeSocket() {
        try {
            conf.playerSocket?.disconnect()
        } catch (e: Exception) {
            Timber.e(e, "Failed to disconnect.")
        }
    }

    fun skip(toIndex: Int) {
        conf.player.skip(toIndex)
    }

    fun remove(idx: Int) {
        conf.player.remove(idx)
    }

    fun play(track: Track) {
        conf.player.play(track)
    }

    fun add(id: Track) {
        conf.player.add(id)
    }

    fun addFolder(id: FolderId) {
        conf.player.addFolder(id)
    }

    private fun checkPlaybackPosition(): Boolean = handler.postDelayed({
        val currPosition = latestState.currentPlaybackPosition
        if (timeUpdates.value != currPosition)
            times.postValue(currPosition)
        if (updatePosition)
            checkPlaybackPosition()

    }, pollInterval.toMillis().toLong())
}

inline val PlaybackStateCompat.currentPlaybackPosition: Duration
    get() = if (state == PlaybackStateCompat.STATE_PLAYING) {
        val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        Duration(0.001 * (position + (timeDelta * playbackSpeed)))
    } else {
        Duration(0.001 * position)
    }
