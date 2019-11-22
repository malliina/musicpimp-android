package org.musicpimp.ui.music

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_music.view.*
import kotlinx.android.synthetic.main.fragment_player.view.*
import org.musicpimp.Directory
import org.musicpimp.MainActivityViewModel
import org.musicpimp.R
import org.musicpimp.Track
import timber.log.Timber

abstract class CommonMusicFragment : Fragment(), MusicItemDelegate {
    private lateinit var viewAdapter: MusicAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var mainViewModel: MainActivityViewModel
    protected var viewModel: MusicViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        viewManager = LinearLayoutManager(context)
        viewAdapter = MusicAdapter(Directory.empty, this)
        view.tracks_view.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        if (mainViewModel.http == null) {
            display(getString(R.string.no_music), view)
        }
        mainViewModel.http?.let { httpClient ->
            val vm = ViewModelProviders.of(
                this,
                MusicViewModelFactory(requireActivity().application, httpClient)
            ).get(MusicViewModel::class.java)
            vm.directory.observe(viewLifecycleOwner) { outcome ->
                when (outcome.status) {
                    Status.Success -> {
                        view.music_progress.visibility = View.GONE
                        view.tracks_view.visibility = View.VISIBLE
                        view.no_music_text.visibility = View.GONE
                        outcome.data?.let { dir ->
                            if (dir.isEmpty) {
                                display(getString(R.string.no_music), view)
                            } else {
                                viewAdapter.directory = dir
                                viewAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                    Status.Error -> {
                        display(outcome.error?.message ?: getString(R.string.error_generic), view)
                    }
                    Status.Loading -> {
                        view.music_progress.visibility = View.VISIBLE
                        view.tracks_view.visibility = View.GONE
                        view.no_music_text.visibility = View.GONE
                    }
                }
            }
            viewModel = vm
        }
        setHasOptionsMenu(true)
    }

    private fun display(message: String, view: View) {
        view.music_progress.visibility = View.GONE
        view.tracks_view.visibility = View.GONE
        view.no_music_text.visibility = View.VISIBLE
        view.no_music_text.text = message
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.music_top_nav_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(
            findNavController(
                requireActivity(),
                R.id.nav_host_container
            )
        )
                || super.onOptionsItemSelected(item)
    }

    override fun onTrack(track: Track) {
        mainViewModel.socket?.play(track.id)
    }
}
