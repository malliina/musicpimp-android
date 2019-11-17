package org.musicpimp.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.fragment_player.view.*
import org.musicpimp.MainActivityViewModel
import org.musicpimp.Playstate
import org.musicpimp.R
import timber.log.Timber
import java.lang.IllegalArgumentException

class PlayerFragment : Fragment() {
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var playerViewModel: PlayerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        playerViewModel =
            ViewModelProviders.of(this).get(PlayerViewModel::class.java)
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        mainViewModel.timeUpdates.observe(viewLifecycleOwner) {
            val float = it.seconds.toFloat()
            try {
                view.position_text.text = it.formatted()
                val slider = view.player_slider
                if (float >= slider.min && float <= slider.max) {
                    slider.progress = it.seconds.toInt()
                } else {
                    Timber.w("Out of bounds: $float.")
                }
            } catch (iae: IllegalArgumentException) {
                Timber.e("Invalid position: '${it.seconds}'.")
            }
        }
        mainViewModel.trackUpdates.observe(viewLifecycleOwner) {
            view.track_text.text = it.title
            view.album_text.text = it.album
            view.artist_text.text = it.artist
            view.duration_text.text = it.duration.formatted()
            view.player_slider.max = it.duration.seconds.toInt()
        }
        mainViewModel.stateUpdates.observe(viewLifecycleOwner) {
            view.no_track_text.visibility = if (it == Playstate.NoMedia) View.VISIBLE else View.GONE
            view.playback_controls.visibility =
                if (it != Playstate.NoMedia) View.VISIBLE else View.GONE
        }
    }
}
