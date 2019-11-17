package org.musicpimp.ui.music

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.musicpimp.*

class MusicFragment : CommonMusicFragment() {
    private val args: MusicFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadFolder(args.folderId)
    }

    override fun onFolder(folder: Folder) {
        findNavController().navigate(MusicFragmentDirections.musicToMusic(folder.id, folder.title))
    }
}
