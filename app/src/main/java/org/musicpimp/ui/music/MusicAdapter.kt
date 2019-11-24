package org.musicpimp.ui.music

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
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
    fun onTrackMore(track: Track, view: ImageButton)
    fun onFolderMore(folder: Folder, view: ImageButton)
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
        val moreButton = layout.music_item_more_button
        val title = layout.music_item_title
        if (position < directory.folders.size) {
            val folder = directory.folders[position]
            title.text = folder.title
            layout.setOnClickListener {
                delegate.onFolder(folder)
            }
            moreButton.setOnClickListener {
                delegate.onFolderMore(folder, moreButton)
            }
        } else {
            val track = directory.tracks[position - directory.folders.size]
            title.text = track.title
            layout.setOnClickListener {
                delegate.onTrack(track)
            }
            moreButton.setOnClickListener {
                delegate.onTrackMore(track, moreButton)
            }
        }
    }

    override fun getItemCount(): Int = directory.size
}
