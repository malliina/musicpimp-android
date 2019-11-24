package org.musicpimp.ui.music

import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_music.view.*
import org.musicpimp.*
import timber.log.Timber

abstract class CommonMusicFragment : Fragment(), MusicItemDelegate {
    private lateinit var viewAdapter: MusicAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var mainViewModel: MainActivityViewModel
    protected lateinit var viewModel: MusicViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        viewManager = LinearLayoutManager(context)
        viewAdapter = MusicAdapter(Directory.empty, this)
        view.tracks_view.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        if (mainViewModel.http == null) {
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
        setHasOptionsMenu(true)
    }

    private fun display(message: String, view: View) {
        view.music_progress.visibility = View.GONE
        view.tracks_view.visibility = View.GONE
        view.no_music_text.visibility = View.VISIBLE
        view.no_music_text.text = message
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.music_top_nav_menu, menu)
    }

    override fun onTrack(track: Track) {
        mainViewModel.playerSocket?.play(track.id)
    }

    override fun onTrackMore(track: Track, view: ImageButton) {
        showPopup(view) {
            Timber.i("Add track ${track.id}")
            mainViewModel.playerSocket?.add(track.id)
        }
    }

    override fun onFolderMore(folder: Folder, view: ImageButton) {
        showPopup(view) {
            mainViewModel.playerSocket?.addFolder(folder.id)
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
