package org.musicpimp.ui.player

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
import kotlinx.android.synthetic.main.fragment_playlist.view.*
import org.musicpimp.*
import org.musicpimp.ui.music.TrackDelegate
import timber.log.Timber

class PlaylistFragment : Fragment(), TrackDelegate {
    private lateinit var viewAdapter: PlaylistAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var viewModel: PlaylistViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        viewModel = ViewModelProviders.of(
            this,
            PlaylistViewModelFactory(requireActivity().application, mainViewModel)
        ).get(PlaylistViewModel::class.java)

        viewManager = LinearLayoutManager(context)
        viewAdapter = PlaylistAdapter(emptyList(), -1, requireContext(), this)
        view.playlist_list.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        mainViewModel.playlistUpdates.observe(viewLifecycleOwner) { list ->
            Timber.i("Got playlist with ${list.size} tracks"    )
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
        showPopup(view) {
            mainViewModel.remove(position)
        }
    }

    private fun showPopup(v: View, onRemove: () -> Unit) {
        val popup = PopupMenu(requireContext(), v)
        popup.inflate(R.menu.remove_menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.remove_from_playlist_item -> {
                    onRemove()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
