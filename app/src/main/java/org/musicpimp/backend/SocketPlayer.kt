package org.musicpimp.backend

import org.musicpimp.*
import org.musicpimp.audio.Player

class SocketPlayer(private val socket: PimpSocket) : Player {
    private val adapters: PimpSocket.Companion.Adapters = PimpSocket.Companion.Adapters
    override fun play(track: Track) = trackCommand("play", track.id)
    override fun add(track: Track) = trackCommand("add", track.id)
    override fun addAll(tracks: List<Track>) = socket.send(
        ItemsCommand("add_items", tracks.map { t -> t.id }, emptyList()),
        adapters.items
    )
    override fun resume() = simple("resume")
    override fun stop() = simple("stop")
    override fun pause() = simple("stop")
    override fun next() = simple("next")
    override fun prev() = simple("prev")
    override fun skip(idx: Int) = valueCommand("skip", idx)
    override fun remove(idx: Int) = valueCommand("remove", idx)
    override fun seek(to: Duration) = valueCommand("seek", to.seconds.toInt())

    fun status() = simple("status")

    private fun valueCommand(cmd: String, value: Int) =
        socket.send(ValueCommand(cmd, value), adapters.valueCmd)

    private fun trackCommand(cmd: String, track: TrackId) =
        socket.send(TrackCommand(cmd, track), adapters.trackCmd)

    private fun simple(cmd: String) =
        socket.send(SimpleCommand(cmd), adapters.simple)
}
