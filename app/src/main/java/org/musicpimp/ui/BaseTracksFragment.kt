package org.musicpimp.ui

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.musicpimp.MainActivityViewModel
import org.musicpimp.R
import org.musicpimp.ui.music.TrackDelegate
import org.musicpimp.ui.playlists.PimpAdapter
import org.musicpimp.ui.playlists.TracksViewModel

abstract class BaseTracksFragment<T, A : PimpAdapter<T>, V : TracksViewModel<T>>(private val fragmentResource: Int) :
    Fragment(), TrackDelegate {
    // Number of tracks to load per "page" (we use infinite scroll)
    protected val itemsPerLoad = 40
    protected val mainViewModel: MainActivityViewModel by activityViewModels()
    protected lateinit var viewAdapter: A
    protected lateinit var viewManager: LinearLayoutManager
    protected lateinit var viewModel: V

    abstract fun newViewModel(fragment: Fragment, app: Application): V
    abstract fun newAdapter(context: Context): A
    abstract fun init(view: View, viewManager: LinearLayoutManager, adapter: A)
    abstract fun controls(view: View): Controls

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(fragmentResource, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctrl = controls(view)
        viewManager = LinearLayoutManager(context)
        viewAdapter = newAdapter(requireContext())
        init(view, viewManager, viewAdapter)
        viewModel = newViewModel(this, requireActivity().application)
        viewModel.tracks.observe(viewLifecycleOwner) { outcome ->
            when (outcome.status) {
                Status.Success -> {
                    ctrl.showList()
                    outcome.data?.let { list ->
                        if (list.isEmpty()) {
                            ctrl.display(getString(R.string.no_tracks))
                        } else {
                            viewAdapter.list = list
                            viewAdapter.notifyDataSetChanged()
                        }
                    }
                }
                Status.Error -> {
                    ctrl.display(getString(R.string.error_generic))
                }
                Status.Loading -> {
                    ctrl.enableLoading()
                }
            }
        }
        viewModel.loadTracks(0, itemsPerLoad)
    }
}
