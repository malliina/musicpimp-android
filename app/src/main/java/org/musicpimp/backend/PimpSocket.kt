package org.musicpimp.backend

import com.squareup.moshi.JsonAdapter
import org.musicpimp.*
import org.musicpimp.Json.Companion.moshi
import timber.log.Timber
import org.musicpimp.PlaybackEvent.*

interface SocketDelegate {
    fun timeUpdated(time: Duration)
    fun trackUpdated(track: Track)
    fun playlistUpdated(list: List<Track>)
    fun indexUpdated(idx: Int)
    fun playstateUpdated(state: Playstate)
    fun onStatus(status: StatusMessage)
}

class PimpSocket(url: FullUrl, headers: Map<String, String>, private val delegate: SocketDelegate) :
    WebSocketClient(url, headers) {
    companion object {
        object adapters {
            val event: JsonAdapter<ServerEvent> = moshi.adapter(ServerEvent::class.java)
            val position: JsonAdapter<PositionMessage> = moshi.adapter(PositionMessage::class.java)
            val track: JsonAdapter<TrackMessage> = moshi.adapter(TrackMessage::class.java)
            val playstate: JsonAdapter<PlaystateMessage> =
                moshi.adapter(PlaystateMessage::class.java)
            val playlist: JsonAdapter<PlaylistModifiedMessage> =
                moshi.adapter(PlaylistModifiedMessage::class.java)
            val index: JsonAdapter<IndexChangedMessage> =
                moshi.adapter(IndexChangedMessage::class.java)
            val volume: JsonAdapter<VolumeChangedMessage> =
                moshi.adapter(VolumeChangedMessage::class.java)
            val mute: JsonAdapter<MuteMessage> = moshi.adapter(MuteMessage::class.java)
            val status: JsonAdapter<StatusMessage> = moshi.adapter(StatusMessage::class.java)
            val trackCmd: JsonAdapter<TrackCommand> = moshi.adapter(TrackCommand::class.java)
            val simple: JsonAdapter<SimpleCommand> = moshi.adapter(SimpleCommand::class.java)
            val items: JsonAdapter<ItemsCommand> = moshi.adapter(ItemsCommand::class.java)
            val valueCmd: JsonAdapter<ValueCommand> = moshi.adapter(ValueCommand::class.java)
        }

        fun build(auth: AuthHeader, delegate: SocketDelegate): PimpSocket {
            val socketUrl = baseUrl.append("/ws/playback")
            Timber.i("Setting socketUrl to '$socketUrl'.")
            return PimpSocket(socketUrl, HttpClient.headers(auth), delegate)
        }
    }

    override fun onConnected(url: FullUrl) {
        status()
    }

    override fun onMessage(message: String) {
        Timber.i("Got '$message'.")
        adapters.event.fromJson(message)?.let {
            when (it.event) {
                Welcome -> OtherMessage(message)
                Ping -> OtherMessage(message)
                TimeUpdated -> {
                    val position = adapters.position.read(message)
                    delegate.timeUpdated(position.position)
                }
                TrackChanged -> {
                    val track = adapters.track.read(message)
                    delegate.trackUpdated(track.track)
                }
                PlaystateChanged -> {
                    val state = adapters.playstate.read(message)
                    delegate.playstateUpdated(state.state)
                }
                PlaylistModified -> {
                    val list = adapters.playlist.read(message)
                    delegate.playlistUpdated(list.playlist)
                }
                PlaylistIndexChanged -> {
                    val idx = adapters.index.read(message)
                    delegate.indexUpdated(idx.playlist_index)
                }
                VolumeChanged -> adapters.volume.read(message)
                MuteToggled -> adapters.mute.read(message)
                Status -> {
                    val status = adapters.status.read(message)
                    delegate.onStatus(status)
                }
                Other -> OtherMessage(message)
            }
        }
    }

    fun play(track: TrackId) = trackCommand("play", track)
    fun add(track: TrackId) = trackCommand("add", track)
    fun addFolder(folder: FolderId) = send(ItemsCommand("add_items", emptyList(), listOf(folder)), adapters.items)
    fun resume() = simple("resume")
    fun stop() = simple("stop")
    fun next() = simple("next")
    fun prev() = simple("prev")
    fun skip(idx: Int) = valueCommand("skip", idx)
    fun remove(idx: Int) = valueCommand("remove", idx)
    fun status() = simple("status")
    fun valueCommand(cmd: String, value: Int) = send(ValueCommand(cmd, value), adapters.valueCmd)
    fun trackCommand(cmd: String, track: TrackId) = send(TrackCommand(cmd, track), adapters.trackCmd)
    fun simple(cmd: String) = send(SimpleCommand(cmd), adapters.simple)
}
