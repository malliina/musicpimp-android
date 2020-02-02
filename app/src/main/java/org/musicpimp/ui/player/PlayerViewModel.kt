package org.musicpimp.ui.player

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.musicpimp.*
import org.musicpimp.Utils.urlEncode
import org.musicpimp.audio.Player
import org.musicpimp.backend.OkClient
import timber.log.Timber
import java.io.File

class PlayerViewModelFactory(val app: Application, val main: MainActivityViewModel) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlayerViewModel(app, main) as T
    }
}

class PlayerViewModel(val app: Application, val main: MainActivityViewModel) :
    AndroidViewModel(app) {
    private val conf = (app as PimpApp).components
    private val coversStream = MutableLiveData<Bitmap?>()
    val covers: LiveData<Bitmap?> = coversStream

    fun updateCover(track: Track) {
        viewModelScope.launch {
            conf.covers.cover(track)?.let { bitmap ->
                coversStream.postValue(bitmap)
            }
        }
    }
}

class CoverService(appContext: Context) {
    private val coversDir = appContext.cacheDir.resolve("covers")

    suspend fun cover(track: Track): Bitmap? {
        return fetchCover(track)?.let { f ->
            BitmapFactory.decodeFile(f.absolutePath)
        }
    }

    private suspend fun fetchCover(track: Track): File? {
        val filenameNoExt = Utils.hashString("${track.artist}-${track.album}")
        val destination = coversDir.resolve("$filenameNoExt.jpg")
        return if (destination.exists()) {
            destination
        } else {
            if (track.artist.isNotBlank() && track.album.isNotBlank()) {
                val artistEnc = urlEncode(track.artist)
                val albumEnc = urlEncode(track.album)
                val url =
                    FullUrl(
                        "https",
                        "api.musicpimp.org",
                        "/covers?artist=$artistEnc&album=$albumEnc"
                    )
                OkClient.default.download(url, destination)?.let { _ ->
                    destination
                }
            } else {
                null
            }
        }
    }
}