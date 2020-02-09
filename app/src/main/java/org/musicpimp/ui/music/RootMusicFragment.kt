package org.musicpimp.ui.music

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import org.musicpimp.Folder
import org.musicpimp.FolderId
import org.musicpimp.R

/**
 * This class exists because I don't know how to get the back button to appear if I use
 * MusicFragment both as a start and non-start destination. So,
 * RootMusicFragment = start destination => no back button,
 * MusicFragment = non-start destination => back button.
 */
class RootMusicFragment : CommonMusicFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadFolder(FolderId.root)
    }

    override fun onFolder(folder: Folder) {
        findNavController().navigate(
            RootMusicFragmentDirections.homeToMusic(folder.id, folder.title)
        )
    }
}
