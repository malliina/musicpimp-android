package org.musicpimp.backend

import com.squareup.moshi.JsonAdapter
import org.musicpimp.*
import org.musicpimp.Json.Companion.moshi
import timber.log.Timber
import org.musicpimp.PlaybackEvent.*
import org.musicpimp.audio.Player

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
        object Adapters {
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

    val player = SocketPlayer(this)

    override fun onConnected(url: FullUrl) {
        player.status()
    }

    override fun onMessage(message: String) {
        Timber.i("Got '$message'.")
        Adapters.event.fromJson(message)?.let {
            when (it.event) {
                Welcome -> OtherMessage(message)
                Ping -> OtherMessage(message)
                TimeUpdated -> {
                    val position = Adapters.position.read(message)
                    delegate.timeUpdated(position.position)
                }
                TrackChanged -> {
                    val track = Adapters.track.read(message)
                    delegate.trackUpdated(track.track)
                }
                PlaystateChanged -> {
                    val state = Adapters.playstate.read(message)
                    delegate.playstateUpdated(state.state)
                }
                PlaylistModified -> {
                    val list = Adapters.playlist.read(message)
                    delegate.playlistUpdated(list.playlist)
                }
                PlaylistIndexChanged -> {
                    val idx = Adapters.index.read(message)
                    delegate.indexUpdated(idx.playlist_index)
                }
                VolumeChanged -> Adapters.volume.read(message)
                MuteToggled -> Adapters.mute.read(message)
                Status -> {
                    val status = Adapters.status.read(message)
                    delegate.onStatus(status)
                }
                Other -> OtherMessage(message)
            }
        }
    }
}

class SocketPlayer(private val socket: PimpSocket) : Player {
    override fun play(track: Track) = trackCommand("play", track.id)
    override fun add(track: Track) = trackCommand("add", track.id)
    override fun resume() = simple("resume")
    override fun stop() = simple("stop")
    override fun next() = simple("next")
    override fun prev() = simple("prev")
    override fun skip(idx: Int) = valueCommand("skip", idx)
    override fun remove(idx: Int) = valueCommand("remove", idx)
    override fun addFolder(folder: FolderId) = socket.send(
        ItemsCommand("add_items", emptyList(), listOf(folder)),
        PimpSocket.Companion.Adapters.items
    )
    override fun seek(to: Duration) = valueCommand("seek", to.seconds.toInt())

    fun status() = simple("status")
    private fun valueCommand(cmd: String, value: Int) =
        socket.send(ValueCommand(cmd, value), PimpSocket.Companion.Adapters.valueCmd)

    private fun trackCommand(cmd: String, track: TrackId) =
        socket.send(TrackCommand(cmd, track), PimpSocket.Companion.Adapters.trackCmd)

    private fun simple(cmd: String) =
        socket.send(SimpleCommand(cmd), PimpSocket.Companion.Adapters.simple)
}
