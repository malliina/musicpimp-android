package org.musicpimp.ui.player

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_playlist.view.*
import org.musicpimp.R
import org.musicpimp.Track
import org.musicpimp.ui.BaseTracksFragment
import org.musicpimp.ui.Controls
import org.musicpimp.ui.init
import timber.log.Timber

class PlaylistFragment :
    BaseTracksFragment<Track, PlaylistAdapter, PlaylistViewModel>(R.layout.fragment_playlist) {
    override fun newViewModel(fragment: Fragment, app: Application): PlaylistViewModel {
        return ViewModelProviders.of(
            this,
            PlaylistViewModelFactory(requireActivity().application, mainViewModel)
        ).get(PlaylistViewModel::class.java)
    }

    override fun newAdapter(context: Context): PlaylistAdapter {
        return PlaylistAdapter(emptyList(), -1, requireContext(), this)
    }

    override fun init(
        view: View,
        viewManager: LinearLayoutManager,
        adapter: PlaylistAdapter
    ) {
        view.playlist_list.init(viewManager, adapter)
    }

    override fun controls(view: View): Controls =
        Controls(null, view.playlist_list, view.empty_playlist_text)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.playlistUpdates.observe(viewLifecycleOwner) { list ->
            Timber.i("Got playlist with ${list.size} tracks")
            viewAdapter.list = list
            viewAdapter.notifyDataSetChanged()
            val emptyText = view.empty_playlist_text
            emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            view.playlist_list.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }
        mainViewModel.indexUpdates.observe(viewLifecycleOwner) { idx ->
            viewAdapter.activeIndex = idx
            viewAdapter.notifyDataSetChanged()
        }
    }

    override fun onTrack(track: Track, position: Int) {
        mainViewModel.skip(position)
    }

    override fun onTrackMore(track: Track, view: ImageButton, position: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.inflate(R.menu.remove_menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.remove_from_playlist_item -> {
                    mainViewModel.remove(position)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
