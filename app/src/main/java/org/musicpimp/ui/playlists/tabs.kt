package org.musicpimp.ui.playlists

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_playlists.view.*
import kotlinx.android.synthetic.main.fragment_popular.view.*
import kotlinx.android.synthetic.main.fragment_recent.view.*
import org.musicpimp.PopularTrack
import org.musicpimp.R
import org.musicpimp.RecentTrack
import org.musicpimp.ui.Controls
import org.musicpimp.ui.ResourceFragment
import org.musicpimp.ui.TopTracksFragment
import org.musicpimp.ui.init

class PlaylistsFragment : ResourceFragment(R.layout.fragment_playlists) {
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
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
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

class PopularFragment :
    TopTracksFragment<PopularTrack, PopularsAdapter, PopularsViewModel>(R.layout.fragment_popular) {
    override fun newViewModel(fragment: Fragment, app: Application): PopularsViewModel {
        return ViewModelProvider(
            this,
            PopularsViewModelFactory(app)
        ).get(PopularsViewModel::class.java)
    }

    override fun newAdapter(context: Context): PopularsAdapter =
        PopularsAdapter(emptyList(), context, this)

    override fun init(
        view: View,
        viewManager: LinearLayoutManager,
        adapter: PopularsAdapter
    ) {
        view.popular_list.init(viewManager, adapter)
        installInfiniteScroll(view.popular_list)
    }

    override fun controls(view: View): Controls =
        Controls(null, view.popular_list, view.popular_feedback)
}

class RecentFragment :
    TopTracksFragment<RecentTrack, RecentsAdapter, RecentsViewModel>(R.layout.fragment_recent) {
    override fun newViewModel(fragment: Fragment, app: Application): RecentsViewModel {
        return ViewModelProvider(
            this,
            RecentsViewModelFactory(app)
        ).get(RecentsViewModel::class.java)
    }

    override fun newAdapter(context: Context): RecentsAdapter =
        RecentsAdapter(emptyList(), context, this)

    override fun init(
        view: View,
        viewManager: LinearLayoutManager,
        adapter: RecentsAdapter
    ) {
        view.recent_list.init(viewManager, adapter)
        installInfiniteScroll(view.recent_list)
    }

    override fun controls(view: View): Controls =
        Controls(null, view.recent_list, view.recent_feedback)
}
