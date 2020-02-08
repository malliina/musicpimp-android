package org.musicpimp.ui.playlists

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_popular.view.*
import kotlinx.android.synthetic.main.item_recent.view.*
import org.musicpimp.PopularTrack
import org.musicpimp.R
import org.musicpimp.RecentTrack
import org.musicpimp.ui.music.TrackDelegate
import java.time.Instant

class PopularsAdapter(
    initial: List<PopularTrack>,
    private val context: Context,
    private val delegate: TrackDelegate
) : PimpAdapter<PopularTrack>(initial, R.layout.item_popular) {

    override fun onBindViewHolder(holder: TopHolder, position: Int) {
        val layout = holder.layout
        val popTrack = list[position]
        val track = popTrack.track
        layout.popular_title.text = track.title
        layout.popular_artist.text = track.artist.value
        layout.popular_plays.text =
            context.resources.getString(R.string.popular_plays, popTrack.playbackCount)
        layout.setOnClickListener {
            delegate.onTrack(track, position)
        }
        val moreButton = layout.popular_more_button
        moreButton.setOnClickListener {
            delegate.onTrackMore(track, moreButton, position)
        }
    }
}

class RecentsAdapter(
    initial: List<RecentTrack>,
    private val context: Context,
    private val delegate: TrackDelegate
) : PimpAdapter<RecentTrack>(initial, R.layout.item_recent) {

    override fun onBindViewHolder(holder: TopHolder, position: Int) {
        val layout = holder.layout
        val recentTrack = list[position]
        val track = recentTrack.track
        layout.recent_title.text = track.title
        layout.recent_artist.text = track.artist.value
        layout.recent_timestamp.text = formattedTime(recentTrack.timestamp)
        layout.setOnClickListener {
            delegate.onTrack(track, position)
        }
        val moreButton = layout.recent_more_button
        moreButton.setOnClickListener {
            delegate.onTrackMore(track, moreButton, position)
        }
    }

    private fun formattedTime(i: Instant): String =
        DateUtils.formatDateTime(
            context,
            i.toEpochMilli(),
            DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        )
}

abstract class PimpAdapter<T>(
    var list: List<T>,
    private val itemResource: Int
) : RecyclerView.Adapter<PimpAdapter.TopHolder>() {
    class TopHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopHolder {
        val layout = LayoutInflater.from(parent.context).inflate(
            itemResource,
            parent,
            false
        ) as ConstraintLayout
        return TopHolder(layout)
    }

    override fun getItemCount(): Int = list.size
}
