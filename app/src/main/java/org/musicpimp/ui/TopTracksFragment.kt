package org.musicpimp.ui

import android.widget.ImageButton
import android.widget.PopupMenu
import org.musicpimp.R
import org.musicpimp.Track
import org.musicpimp.TrackContainer
import org.musicpimp.ui.playlists.PimpAdapter
import org.musicpimp.ui.playlists.TracksViewModel

abstract class TopTracksFragment<T: TrackContainer, A : PimpAdapter<T>, V : TracksViewModel<T>>(fragmentResource: Int) :
    BaseTracksFragment<T, A, V>(fragmentResource) {

    override fun onTrack(track: Track, position: Int) {
        mainViewModel.play(track)
    }

    override fun onTrackMore(track: Track, view: ImageButton, position: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.inflate(R.menu.top_item_menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.top_add_item -> {
                    mainViewModel.add(track)
                    true
                }
                R.id.start_here_item -> {
                    mainViewModel.playAll(viewAdapter.list.drop(position).map { it.track })
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
