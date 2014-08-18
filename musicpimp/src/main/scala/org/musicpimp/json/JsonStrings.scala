package org.musicpimp.json

/**
 *
 * @author mle
 */
trait JsonStrings {

  val EMPTY_ARRAY = "[]"

  val ID = "id"
  // commands
  val CMD = "cmd"
  val VALUE = "value"
  val TRACK = "track"
  val TRACK_CAPITAL_T = "Track"
  val PLAY = "play"
  val RESUME = "resume"
  val STOP = "stop"
  val ADD = "add"
  val REMOVE = "remove"
  val NEXT = "next"
  val PREV = "prev"
  val SEEK = "seek"
  val SKIP = "skip"

  val CONNECTED = "connected"
  val DISCONNECTED = "disconnected"

  // events
  val EVENT = "event"
  val STATUS = "status"
  val SHORT_STATUS = "short_status"
  val WELCOME = "welcome"
  val TRACK_CHANGED = "track_changed"
  val TIME_UPDATED = "time_updated"
  val VOLUME_CHANGED = "volume_changed"
  val MUTE_TOGGLED = "mute_toggled"
  val PLAYLIST_MODIFIED = "playlist_modified"
  val PLAYLIST_INDEX_CHANGED = "playlist_index_changed"
  val PLAYSTATE_CHANGED = "playstate_changed"
  val SUSPENDED_GETTING_DATA = "suspended_getting_data"

  // parameters
  val TITLE = "title"
  val ALBUM = "album"
  val ARTIST = "artist"
  val SIZE = "size"
  val POSITION = "position"
  val DURATION = "duration"
  val STATE = "state"
  val PATH = "path"
  val VOLUME = "volume"
  val GAIN = "gain"
  val MUTE = "mute"
  val PLAYLIST = "playlist"
  val PLAYLIST_INDEX = "playlist_index"
  val INDEX = "index"
}

object JsonStrings extends JsonStrings
