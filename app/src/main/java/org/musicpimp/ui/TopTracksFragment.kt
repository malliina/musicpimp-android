package org.musicpimp.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import org.musicpimp.R
import org.musicpimp.Track
import org.musicpimp.TrackContainer
import org.musicpimp.ui.playlists.PimpAdapter
import org.musicpimp.ui.playlists.TracksViewModel

abstract class TopTracksFragment<T : TrackContainer, A : PimpAdapter<T>, V : TracksViewModel<T>>(
    fragmentResource: Int
) : BaseTracksFragment<T, A, V>(fragmentResource) {
    // Number of items left until more items are loaded
    private val loadMoreThreshold = 20

    protected val lastVisibleIndex = MutableLiveData<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Transformations.distinctUntilChanged(lastVisibleIndex)
            .observe(viewLifecycleOwner) { lastPos ->
                val itemCount = viewAdapter.itemCount
                if (lastPos + loadMoreThreshold == itemCount) {
                    viewModel.loadTracks(itemCount, itemCount + itemsPerLoad)
                }
            }
    }

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

    fun installInfiniteScroll(list: RecyclerView) {
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    lastVisibleIndex.postValue(viewManager.findLastVisibleItemPosition())
                }
            }
        })
    }
}
