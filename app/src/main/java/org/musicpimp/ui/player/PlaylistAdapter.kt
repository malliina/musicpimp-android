package org.musicpimp.ui.player

import android.content.Context
import kotlinx.android.synthetic.main.track_item.view.*
import org.musicpimp.R
import org.musicpimp.Track
import org.musicpimp.ui.music.TrackDelegate
import org.musicpimp.ui.playlists.PimpAdapter

class PlaylistAdapter(
    initial: List<Track>,
    var activeIndex: Int,
    val context: Context,
    private val delegate: TrackDelegate
) : PimpAdapter<Track>(initial, R.layout.track_item) {

    override fun onBindViewHolder(th: TopHolder, position: Int) {
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
