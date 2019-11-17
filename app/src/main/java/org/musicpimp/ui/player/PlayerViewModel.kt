package org.musicpimp.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.musicpimp.*

class PlayerViewModel : ViewModel() {
    val testTrack = Track(
        TrackId("test"),
        "Title 1",
        Album("21"),
        Artist("Adele"),
        "a/b/c",
        Duration(120.0),
        12121212,
        FullUrl("https", "musicpimp.org", "")
    )
    private val tracksData = MutableLiveData<Track>().apply {
        value = testTrack
    }

    val tracks: LiveData<Track> = tracksData
}
