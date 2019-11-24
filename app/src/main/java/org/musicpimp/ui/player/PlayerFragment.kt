package org.musicpimp.ui.player

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_player.view.*
import org.musicpimp.MainActivityViewModel
import org.musicpimp.Playstate
import org.musicpimp.R
import timber.log.Timber

class PlayerFragment : Fragment() {
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var viewModel: PlayerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

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
            view.album_cover.setImageResource(R.drawable.ic_launcher_foreground)
            view.album_cover.setImageBitmap(cover)
        }
        view.play_button.setOnClickListener {
            viewModel.onPlay()
        }
        view.pause_button.setOnClickListener {
            viewModel.onPause()
        }
        view.next_button.setOnClickListener {
            viewModel.onNext()
        }
        view.prev_button.setOnClickListener {
            viewModel.onPrevious()
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.player_top_nav_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.playlist -> {
                val action = PlayerFragmentDirections.playerToPlaylist()
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
