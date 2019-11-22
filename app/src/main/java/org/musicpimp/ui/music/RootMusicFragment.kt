package org.musicpimp.ui.music

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import org.musicpimp.Folder
import org.musicpimp.FolderId

class RootMusicFragment : CommonMusicFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel?.loadFolder(FolderId.root)
    }

    override fun onFolder(folder: Folder) {
        findNavController().navigate(
            RootMusicFragmentDirections.homeToMusic(
                folder.id,
                folder.title
            )
        )
    }
}
