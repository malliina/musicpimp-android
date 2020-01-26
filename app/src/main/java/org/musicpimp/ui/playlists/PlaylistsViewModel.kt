package org.musicpimp.ui.playlists

import android.app.Application
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_popular.view.*
import kotlinx.android.synthetic.main.item_recent.view.*
import kotlinx.coroutines.launch
import org.musicpimp.*
import org.musicpimp.R
import org.musicpimp.ui.music.Outcome
import org.musicpimp.ui.music.TrackDelegate
import timber.log.Timber
import java.lang.Exception

class PopularsViewModelFactory(val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PopularsViewModel(app) as T
    }
}

class RecentsViewModelFactory(val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return RecentsViewModel(app) as T
    }
}

abstract class TracksViewModel<T>(val app: Application) : AndroidViewModel(app) {
    private val conf = (app as PimpApp).components
    private val data = MutableLiveData<Outcome<List<T>>>()
    val tracks: LiveData<Outcome<List<T>>> = data
    // TODO make PimpComponents return non-null modules
    val http = requireNotNull(conf.http)

    abstract suspend fun load(from: Int, until: Int): List<T>

    fun loadTracks() {
        viewModelScope.launch {
            data.value = Outcome.loading()
            try {
                val items = load(0, 100)
                data.value = Outcome.success(items)
                Timber.i("Loaded recent tracks.")
            } catch (e: Exception) {
                data.value = Outcome.error(SingleError.backend("Error."))
            }
        }
    }
}

class PopularsViewModel(app: Application) : TracksViewModel<PopularTrack>(app) {
    override suspend fun load(from: Int, until: Int): List<PopularTrack> =
        http.popular(from, until).populars
}

class RecentsViewModel(app: Application) : TracksViewModel<RecentTrack>(app) {
    override suspend fun load(from: Int, until: Int): List<RecentTrack> =
        http.recent(from, until).recents
}

class PopularsAdapter(initial: List<PopularTrack>, private val delegate: TrackDelegate) :
    PimpAdapter<PopularTrack>(initial, R.layout.item_popular) {
    override fun onBindViewHolder(holder: TopHolder, position: Int) {
        val layout = holder.layout
        val track = list[position].track
        layout.popular_title.text = track.title
        layout.setOnClickListener {
            delegate.onTrack(track, position)
        }
        val moreButton = layout.popular_more_button
        moreButton.setOnClickListener {
            delegate.onTrackMore(track, moreButton, position)
        }
    }
}

class RecentsAdapter(initial: List<RecentTrack>, private val delegate: TrackDelegate) :
    PimpAdapter<RecentTrack>(initial, R.layout.item_recent) {

    override fun onBindViewHolder(holder: TopHolder, position: Int) {
        val layout = holder.layout
        val track = list[position].track
        layout.recent_title.text = track.title
        layout.setOnClickListener {
            delegate.onTrack(track, position)
        }
        val moreButton = layout.recent_more_button
        moreButton.setOnClickListener {
            delegate.onTrackMore(track, moreButton, position)
        }
    }
}

abstract class PimpAdapter<T>(
    var list: List<T>,
    private val itemResource: Int
) :
    RecyclerView.Adapter<PimpAdapter.TopHolder>() {
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
