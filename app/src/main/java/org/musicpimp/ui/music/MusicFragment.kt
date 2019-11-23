package org.musicpimp.ui.music

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.musicpimp.Folder
import org.musicpimp.FolderId
import org.musicpimp.R

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val action = MusicFragmentDirections.musicToSettings()
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
