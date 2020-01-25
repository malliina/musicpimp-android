package org.musicpimp.ui.player

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.track_item.view.*
import org.musicpimp.R
import org.musicpimp.Track
import org.musicpimp.ui.music.TrackDelegate

class PlaylistAdapter(
    var list: List<Track>,
    var activeIndex: Int,
    val context: Context,
    private val delegate: TrackDelegate
) :
    RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {
    class PlaylistHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder {
        val layout = LayoutInflater.from(parent.context).inflate(
            R.layout.track_item,
            parent,
            false
        ) as ConstraintLayout
        return PlaylistHolder(layout)
    }

    override fun onBindViewHolder(th: PlaylistHolder, position: Int) {
        val layout = th.layout
        val moreButton = layout.music_item_more_button
        val title = layout.music_item_title
        val track = list[position]
        title.text = track.title
        val color = if (position == activeIndex) R.color.colorSelected else R.color.colorTitles
        title.setTextColor(context.getColor(color))
        layout.setOnClickListener {
            delegate.onTrack(track, position)
        }
        moreButton.setOnClickListener {
            delegate.onTrackMore(track, moreButton, position)
        }
    }

    override fun getItemCount(): Int = list.size
}