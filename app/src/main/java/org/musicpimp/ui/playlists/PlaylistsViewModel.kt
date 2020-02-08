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
    val http = requireNotNull(conf.library)

    abstract suspend fun load(from: Int, until: Int): List<T>

    fun loadTracks(from: Int, until: Int) {
        viewModelScope.launch {
            if (from == 0) {
                data.value = Outcome.loading()
            }
            try {
                val items = load(from, until)
                val newList =
                    if (from == 0) items
                    else (data.value?.data ?: emptyList()) + items
                data.value = Outcome.success(newList)
                Timber.i("Loaded tracks from $from until $until, got ${items.size} items.")
            } catch (e: Exception) {
                if (from == 0) {
                    data.value = Outcome.error(SingleError.backend("Error."))
                }
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
