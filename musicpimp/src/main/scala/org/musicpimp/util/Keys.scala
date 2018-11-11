package org.musicpimp.util

trait Keys {
  val FOLDER = "org.musicpimp.folder"
  val PATH = "org.musicpimp.path"
  val ENDPOINT = "org.musicpimp.endpoint"
  val EDITED_FOLDER = "org.musicpimp.editedFolder"
  val ALARM_ID = "org.musicpimp.alarmid"
  val TRACK_ID = "org.musicpimp.track.id"
  val TRACK_TITLE = "org.musicpimp.track.title"

  val PREF_ENDPOINTS = "endpoints3"
  val PREF_BEAM = "beam"
  val PREF_LIBRARY = "pref_source"
  val PREF_FOLDERS = "pref_folders"
  val PREF_PLAYER = "pref_player"
  val PREF_CACHE = "pref_cache"

  val PREF_UNLIMITED_PLAYBACK = "pref_unlimited"
  val PREF_REMOTE_PLAYBACK = "pref_remotes"
  val PREF_LOCAL_PLAYBACK = "pref_locals"
  val PREF_PLAYBACK_COUNT = "pref_upfront"

  val PREF_FIRST_USE = "PREF_FIRST_USE"

  val PREF_NOTIFICATIONS_ENABLED = "pref_notifications"
}

object Keys extends Keys
