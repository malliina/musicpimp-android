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

class SeekBarChangeListener(private val vm: MainActivityViewModel) :
    SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        // TODO Disable advancing the seek bar
//        Timber.i("Started at ${seekBar.progress}")
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        // TODO Enable advancing the seek bar
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

class PlayerFragment : ResourceFragment(R.layout.fragment_player) {
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var viewModel: PlayerViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        viewModel = ViewModelProviders.of(
            this,
            PlayerViewModelFactory(requireActivity().application, mainViewModel)
        ).get(PlayerViewModel::class.java)
        mainViewModel.timeUpdates.observe(viewLifecycleOwner) { time ->
            val float = time.seconds.toFloat()
            try {
                view.position_text.text = time.formatted()
                val slider = view.player_slider
                if (float >= slider.min && float <= slider.max) {
                    slider.progress = time.seconds.toInt()
                } else {
                    Timber.w("Out of bounds: $float.")
                }
            } catch (iae: IllegalArgumentException) {
                Timber.e("Invalid position: '${time.seconds}'.")
            }
        }
        mainViewModel.trackUpdates.observe(viewLifecycleOwner) { track ->
            view.track_text.text = track.title
            view.album_text.text = track.album
            view.artist_text.text = track.artist
            view.duration_text.text = track.duration.formatted()
            view.player_slider.max = track.duration.seconds.toInt()
            viewModel.updateCover(track)
        }
        mainViewModel.stateUpdates.observe(viewLifecycleOwner) { state ->
            view.no_track_text.visibility =
                if (state == Playstate.NoMedia) View.VISIBLE else View.GONE
            view.playback_controls.visibility =
                if (state != Playstate.NoMedia) View.VISIBLE else View.GONE
            if (state == Playstate.Playing) {
                view.pause_button.visibility = View.VISIBLE
                view.play_button.visibility = View.GONE
            } else {
                view.pause_button.visibility = View.GONE
                view.play_button.visibility = View.VISIBLE
            }
        }
        viewModel.covers.observe(viewLifecycleOwner) { cover ->
            if (cover == null) {
                view.album_cover.setImageResource(R.drawable.ic_launcher_foreground)
            } else {
                view.album_cover.setImageBitmap(cover)
            }
        }
        view.play_button.setOnClickListener {
            mainViewModel.resume()
        }
        view.pause_button.setOnClickListener {
            mainViewModel.pause()
        }
        view.next_button.setOnClickListener {
            mainViewModel.next()
        }
        view.prev_button.setOnClickListener {
            mainViewModel.previous()
        }
        view.player_slider.setOnSeekBarChangeListener(SeekBarChangeListener(mainViewModel))
        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        mainActivity().toggleControls(block = true)
    }

    override fun onStop() {
        super.onStop()
        mainActivity().toggleControls(block = false)
    }

    private fun mainActivity(): MainActivity = (requireActivity() as MainActivity)
}
