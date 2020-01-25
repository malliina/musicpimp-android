package org.musicpimp.ui.playlists

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_playlists.view.*
import kotlinx.android.synthetic.main.fragment_popular.view.*
import kotlinx.android.synthetic.main.fragment_recent.view.*
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import org.musicpimp.*
import org.musicpimp.ui.init
import org.musicpimp.ui.music.Status
import org.musicpimp.ui.music.TrackDelegate
import timber.log.Timber

class PlaylistsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playlists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            view.playlists_pager.adapter = PlaylistsFragmentAdapter(
                childFragmentManager,
                it.getString(R.string.title_popular),
                it.getString(R.string.title_recent)
            )
        }
    }
}

class PlaylistsFragmentAdapter(
    fm: FragmentManager,
    private val popularTitle: String,
    private val recentTitle: String
) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> PopularFragment()
            1 -> RecentFragment()
            else -> PopularFragment()
        }
    }

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> popularTitle
            1 -> recentTitle
            else -> ""
        }
    }
}

class PopularFragment : TopFragment<PopularTrack, PopularsViewModel>(R.layout.fragment_popular) {
    override fun newViewModel(fragment: Fragment, app: Application): PopularsViewModel {
        return ViewModelProviders.of(
            this,
            PopularsViewModelFactory(requireActivity().application)
        ).get(PopularsViewModel::class.java)
    }

    override fun newAdapter(): TopAdapter<PopularTrack> {
        return PopularsAdapter(emptyList(), this)
    }

    override fun init(
        view: View,
        viewManager: RecyclerView.LayoutManager,
        adapter: TopAdapter<PopularTrack>
    ) {
        view.popular_list.init(viewManager, adapter)
    }

    override fun controls(view: View): Controls =
        Controls(null, view.popular_list, view.popular_feedback)
}

class RecentFragment : TopFragment<RecentTrack, RecentsViewModel>(R.layout.fragment_recent) {
    override fun newViewModel(fragment: Fragment, app: Application): RecentsViewModel {
        return ViewModelProviders.of(
            this,
            RecentsViewModelFactory(requireActivity().application)
        ).get(RecentsViewModel::class.java)
    }

    override fun newAdapter(): TopAdapter<RecentTrack> {
        return RecentsAdapter(emptyList(), this)
    }

    override fun init(
        view: View,
        viewManager: RecyclerView.LayoutManager,
        adapter: TopAdapter<RecentTrack>
    ) {
        view.recent_list.init(viewManager, adapter)
    }

    override fun controls(view: View): Controls =
        Controls(null, view.recent_list, view.recent_feedback)
}

class Controls(val progress: MaterialProgressBar?, val list: RecyclerView, val feedback: TextView)

abstract class TopFragment<T, V : TopViewModel<T>>(private val fragmentResource: Int) : Fragment(),
    TrackDelegate {
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var viewAdapter: TopAdapter<T>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewModel: V

    abstract fun newViewModel(fragment: Fragment, app: Application): V
    abstract fun newAdapter(): TopAdapter<T>
    abstract fun init(view: View, viewManager: RecyclerView.LayoutManager, adapter: TopAdapter<T>)
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
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        val ctrl = controls(view)
        viewManager = LinearLayoutManager(context)
        viewAdapter = newAdapter()
        init(view, viewManager, viewAdapter)
        viewModel = newViewModel(this, requireActivity().application)
        viewModel.tracks.observe(viewLifecycleOwner) { outcome ->
            when (outcome.status) {
                Status.Success -> {
                    ctrl.progress?.let { it.visibility = View.GONE }
                    ctrl.list.visibility = View.VISIBLE
                    ctrl.feedback.visibility = View.GONE
                    outcome.data?.let { list ->
                        if (list.isEmpty()) {
                            display(getString(R.string.no_tracks), ctrl)
                        } else {
                            viewAdapter.list = list
                            viewAdapter.notifyDataSetChanged()
                        }
                    }
                }
                Status.Error -> {
                    display(getString(R.string.error_generic), ctrl)
                }
                Status.Loading -> {
                    ctrl.progress?.let { it.visibility = View.VISIBLE }
                    ctrl.list.visibility = View.GONE
                    ctrl.feedback.visibility = View.GONE
                }
            }
        }
        viewModel.loadTracks()
    }

    private fun display(message: String, ctrl: Controls) {
        ctrl.progress?.let { it.visibility = View.GONE }
        ctrl.list.visibility = View.GONE
        ctrl.feedback.visibility = View.VISIBLE
        ctrl.feedback.text = message
    }

    override fun onTrack(track: Track, position: Int) {
        mainViewModel.play(track)
    }

    override fun onTrackMore(track: Track, view: ImageButton, position: Int) {
        showPopup(view) {
            Timber.i("Add track ${track.id}")
            mainViewModel.add(track)
        }
    }

    private fun showPopup(v: View, onAdd: () -> Unit) {
        val popup = PopupMenu(requireContext(), v)
        popup.inflate(R.menu.top_item_menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.top_add_item -> {
                    onAdd()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
