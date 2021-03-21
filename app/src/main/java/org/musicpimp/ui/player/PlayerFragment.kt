package org.musicpimp.ui.player

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.fragment_player.view.*
import org.musicpimp.*
import org.musicpimp.ui.ResourceFragment
import timber.log.Timber

class PlayerFragment : ResourceFragment(R.layout.fragment_player) {
    private val mainViewModel: MainActivityViewModel by activityViewModels()
    private val viewModel: PlayerViewModel by viewModels()

    var isUserSeeking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel.timeUpdates.observe(viewLifecycleOwner) { time ->
            val float = time.seconds.toFloat()
            try {
                view.position_text.text = time.formatted()
                val slider = view.player_slider
                if (float >= slider.min && float <= slider.max && !isUserSeeking) {
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
        view.player_slider.setOnSeekBarChangeListener(SeekBarChangeListener(this))
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        mainActivity().toggleControls(block = true)
    }

    override fun onPause() {
        super.onPause()
        mainActivity().toggleControls(block = false)
    }

    fun seek(to: Duration) = mainViewModel.seek(to)

    private fun mainActivity(): MainActivity = (requireActivity() as MainActivity)
}
