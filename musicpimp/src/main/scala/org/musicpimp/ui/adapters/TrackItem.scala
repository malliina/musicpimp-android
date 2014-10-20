package org.musicpimp.ui.adapters

import org.musicpimp.audio.{MusicItem, Track}

/**
 * Wraps a track with download progress info so we can display a progress bar under each track as it's being downloaded.
 *
 * @param track the track
 * @param progress download progress of the track
 */
case class TrackItem(track: Track, var progress: DownloadProgress) extends MusicItem {
  val id = track.id
  val title = track.title
  val artist = track.artist
}