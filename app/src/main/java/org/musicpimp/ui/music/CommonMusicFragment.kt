package org.musicpimp.ui.music

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.musicpimp.Directory
import org.musicpimp.MainActivityViewModel
import org.musicpimp.R
import org.musicpimp.Track

abstract class CommonMusicFragment : Fragment(), MusicItemDelegate {
    protected lateinit var viewAdapter: MusicAdapter
    protected lateinit var viewManager: RecyclerView.LayoutManager
    protected lateinit var mainViewModel: MainActivityViewModel
    protected lateinit var viewModel: MusicViewModel

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
        viewModel = ViewModelProviders.of(
            this,
            MusicViewModelFactory(requireActivity().application, mainViewModel.http)
        )
            .get(MusicViewModel::class.java)
        viewManager = LinearLayoutManager(context)
        viewAdapter = MusicAdapter(Directory.empty, this)
        view.findViewById<RecyclerView>(R.id.tracks_view).apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        viewModel.directory.observe(viewLifecycleOwner) {
            viewAdapter.directory = it
            viewAdapter.notifyDataSetChanged()
        }
        setHasOptionsMenu(true)
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
        mainViewModel.socketClient.play(track.id)
    }
}
