package org.musicpimp.ui.fragments

import org.musicpimp.R
import org.musicpimp.audio.{PlayerEvent, PlaylistIndexChanged, PlaylistModified}

class PlaylistFragment extends PlaylistControls with PlaybackFragment {

  override val layoutId: Int = R.layout.playlist

  def onPlayerEvent(event: PlayerEvent) = event match {
    case PlaylistIndexChanged(index) =>
      highlightPlaylistItem(index)
    case PlaylistModified(playlist) =>
      findListView.foreach(listView => showPlaylistItems(listView, playlist))
    case _ => ()
  }

}