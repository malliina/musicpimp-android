package org.musicpimp.ui.music

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.musicpimp.Folder
import org.musicpimp.FolderId

class MusicFragment : CommonMusicFragment() {
    private val args: MusicFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val folderId = args.folderId ?: FolderId.root
        viewModel.loadFolder(folderId)
    }

    override fun onFolder(folder: Folder) {
        findNavController().navigate(MusicFragmentDirections.musicToMusic(folder.id, folder.title))
    }
}
