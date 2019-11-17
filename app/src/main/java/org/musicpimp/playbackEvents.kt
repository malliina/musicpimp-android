package org.musicpimp

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass

class EnumAdapter {
    @FromJson
    fun playbackEvent(e: String): PlaybackEvent = PlaybackEvent.parse(e)
    @FromJson
    fun playstate(e: String): Playstate = Playstate.parse(e)
}

enum class PlaybackEvent(val code: String) {
    Welcome("welcome"),
    Ping("ping"),
    TimeUpdated("time_updated"),
    TrackChanged("track_changed"),
    PlaystateChanged("playstate_changed"),
    PlaylistModified("playlist_modified"),
    PlaylistIndexChanged("playlist_index_changed"),
    VolumeChanged("volume_changed"),
    MuteToggled("mute_toggled"),
    Other("other");

    companion object {
        fun parse(s: String): PlaybackEvent {
            return when (s) {
                "time_updated" -> TimeUpdated
                "track_changed" -> TrackChanged
                "playstate_changed" -> PlaystateChanged
                "playlist_modified" -> PlaylistModified
                "playlist_index_changed" -> PlaylistIndexChanged
                "volume_changed" -> VolumeChanged
                "mute_toggled" -> MuteToggled
                else -> Other
            }
        }
    }
}

enum class Playstate(val code: String) {
    Playing("Playing"),
    Started("Started"),
    Paused("Paused"),
    Stopped("Stopped"),
    NoMedia("NoMedia"),
    Other("Other");

    companion object {
        fun parse(s: String): Playstate {
            return when (s) {
                "Playing" -> Playing
                "Started" -> Started
                "Paused" -> Paused
                "Stopped" -> Stopped
                "NoMedia" -> NoMedia
                else -> Other
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class ServerEvent(val event: PlaybackEvent)

interface ServerMessage

// Server messages
@JsonClass(generateAdapter = true)
data class PositionMessage(val position: Duration) : ServerMessage

@JsonClass(generateAdapter = true)
data class TrackMessage(val track: Track) : ServerMessage

@JsonClass(generateAdapter = true)
data class PlaystateMessage(val state: Playstate) : ServerMessage

@JsonClass(generateAdapter = true)
data class PlaylistModifiedMessage(val playlist: List<Track>) : ServerMessage

@JsonClass(generateAdapter = true)
data class IndexChangedMessage(val playlist_index: Int) : ServerMessage

@JsonClass(generateAdapter = true)
data class VolumeChangedMessage(val volume: Int) : ServerMessage

@JsonClass(generateAdapter = true)
data class MuteMessage(val mute: Boolean) : ServerMessage

data class OtherMessage(val message: String) : ServerMessage

// Client commands
@JsonClass(generateAdapter = true)
data class TrackCommand(val cmd: String, val track: TrackId)

@JsonClass(generateAdapter = true)
data class SimpleCommand(val cmd: String)
