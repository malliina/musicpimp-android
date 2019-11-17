package org.musicpimp.ui.music

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.track_item.view.*
import org.musicpimp.Directory
import org.musicpimp.Folder
import org.musicpimp.R
import org.musicpimp.Track

interface MusicItemDelegate {
    fun onTrack(track: Track)
    fun onFolder(folder: Folder)
}

class MusicAdapter(var directory: Directory, private val delegate: MusicItemDelegate) :
    RecyclerView.Adapter<MusicAdapter.MusicItemHolder>() {
    class MusicItemHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicItemHolder {
        val layout = LayoutInflater.from(parent.context).inflate(
            R.layout.track_item,
            parent,
            false
        ) as ConstraintLayout
        return MusicItemHolder(layout)
    }

    override fun onBindViewHolder(th: MusicItemHolder, position: Int) {
        val layout = th.layout
        if (position < directory.folders.size) {
            val folder = directory.folders[position]
            layout.music_item_title.text = folder.title
            layout.setOnClickListener {
                delegate.onFolder(folder)
            }
        } else {
            val track = directory.tracks[position - directory.folders.size]
            layout.music_item_title.text = track.title
            layout.setOnClickListener {
                delegate.onTrack(track)
            }
        }
    }

    override fun getItemCount(): Int = directory.size
}
