package org.musicpimp.ui.music

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_music.view.*
import org.musicpimp.*
import org.musicpimp.ui.ResourceFragment
import org.musicpimp.ui.init
import timber.log.Timber

abstract class CommonMusicFragment : ResourceFragment(R.layout.fragment_music), MusicItemDelegate {
    private lateinit var viewAdapter: MusicAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private val mainViewModel: MainActivityViewModel by activityViewModels()
    protected val viewModel: MusicViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewManager = LinearLayoutManager(context)
        viewAdapter = MusicAdapter(Directory.empty, this)
        view.tracks_view.init(viewManager, viewAdapter)
        if (mainViewModel.components.library.client == null) {
            display(getString(R.string.no_music), view)
        }
        viewModel.directory.observe(viewLifecycleOwner) { outcome ->
            when (outcome.status) {
                Status.Success -> {
                    view.music_progress.visibility = View.GONE
                    view.tracks_view.visibility = View.VISIBLE
                    view.no_music_text.visibility = View.GONE
                    outcome.data?.let { dir ->
                        if (dir.isEmpty) {
                            display(getString(R.string.no_music), view)
                        } else {
                            viewAdapter.directory = dir
                            viewAdapter.notifyDataSetChanged()
                        }
                    }
                }
                Status.Error -> {
                    outcome.error?.let { Timber.e(it.message) }
                    display(getString(R.string.error_loading_music), view)
                }
                Status.Loading -> {
                    view.music_progress.visibility = View.VISIBLE
                    view.tracks_view.visibility = View.GONE
                    view.no_music_text.visibility = View.GONE
                }
            }
        }
    }

    private fun display(message: String, view: View) {
        view.music_progress.visibility = View.GONE
        view.tracks_view.visibility = View.GONE
        view.no_music_text.visibility = View.VISIBLE
        view.no_music_text.text = message
    }

    override fun onTrack(track: Track, position: Int) {
        mainViewModel.play(track)
    }

    override fun onTrackMore(track: Track, view: ImageButton, position: Int) {
        handlePopup(view, R.menu.track_item_menu) { item ->
            when (item.itemId) {
                R.id.play_track_item -> {
                    mainViewModel.play(track)
                    true
                }
                R.id.add_track_to_playlist_item -> {
                    mainViewModel.add(track)
                    true
                }
                else -> false
            }
        }
    }

    override fun onFolderMore(folder: Folder, view: ImageButton) {
        handlePopup(view, R.menu.music_item_menu) { item ->
            when (item.itemId) {
                R.id.play_folder -> {
                    mainViewModel.playFolder(folder.id)
                    true
                }
                R.id.add_folder_to_playlist_item -> {
                    mainViewModel.addFolder(folder.id)
                    true
                }
                else -> false
            }
        }
    }

    private fun handlePopup(view: ImageButton, menuRes: Int, itemClick: (i: MenuItem) -> Boolean) {
        val popup = PopupMenu(requireContext(), view)
        popup.inflate(menuRes)
        popup.setOnMenuItemClickListener { item ->
            itemClick(item)
        }
        popup.show()
    }
}
