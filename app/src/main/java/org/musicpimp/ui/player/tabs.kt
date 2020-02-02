package org.musicpimp.ui.player

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.fragment_player.view.*
import kotlinx.android.synthetic.main.fragment_tabbed_player.view.*
import org.musicpimp.*
import org.musicpimp.ui.ResourceFragment
import timber.log.Timber

class SeekBarChangeListener(private val vm: PlayerFragment) :
    SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        vm.isUserSeeking = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        vm.isUserSeeking = false
        vm.seek(seekBar.progress.seconds)
    }
}

class TabbedPlayerFragment : ResourceFragment(R.layout.fragment_tabbed_player) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            view.player_pager.adapter = PlayerFragmentAdapter(
                childFragmentManager,
                it.getString(R.string.title_player),
                it.getString(R.string.title_playlist)
            )
        }
    }
}

class PlayerFragmentAdapter(
    fm: FragmentManager,
    private val playerTitle: String,
    private val playlistTitle: String
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> PlayerFragment()
            1 -> PlaylistFragment()
            else -> PlayerFragment()
        }
    }

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> playerTitle
            1 -> playlistTitle
            else -> ""
        }
    }
}
