package org.musicpimp.ui.music

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.lifecycle.ViewModelProviders
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
    private lateinit var mainViewModel: MainActivityViewModel
    protected lateinit var viewModel: MusicViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        viewManager = LinearLayoutManager(context)
        viewAdapter = MusicAdapter(Directory.empty, this)
        view.tracks_view.init(viewManager, viewAdapter)
        if (mainViewModel.components.library == null) {
            display(getString(R.string.no_music), view)
        }
        viewModel = ViewModelProviders.of(
            this,
            MusicViewModelFactory(requireActivity().application, mainViewModel)
        ).get(MusicViewModel::class.java)
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
//        setHasOptionsMenu(true)
    }

    private fun display(message: String, view: View) {
        view.music_progress.visibility = View.GONE
        view.tracks_view.visibility = View.GONE
        view.no_music_text.visibility = View.VISIBLE
        view.no_music_text.text = message
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.music_top_nav_menu, menu)
//    }

    override fun onTrack(track: Track, position: Int) {
        mainViewModel.play(track)
    }

    override fun onTrackMore(track: Track, view: ImageButton, position: Int) {
        showPopup(view) {
            Timber.i("Add track ${track.id}")
            mainViewModel.add(track)
        }
    }

    override fun onFolderMore(folder: Folder, view: ImageButton) {
        showPopup(view) {
            mainViewModel.addFolder(folder.id)
        }
    }

    private fun showPopup(v: View, onAdd: () -> Unit) {
        val popup = PopupMenu(requireContext(), v)
        popup.inflate(R.menu.music_item_menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.add_to_playlist_item -> {
                    onAdd()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
