package org.musicpimp.ui.home

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import org.musicpimp.Folder
import org.musicpimp.FolderId
import org.musicpimp.ui.music.CommonMusicFragment

class HomeFragment : CommonMusicFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadFolder(FolderId.root)
    }

    override fun onFolder(folder: Folder) {
        findNavController().navigate(HomeFragmentDirections.homeToMusic(folder.id, folder.title))
    }
}
