package org.musicpimp.ui.player

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.musicpimp.FullUrl
import org.musicpimp.MainActivityViewModel
import org.musicpimp.Track
import org.musicpimp.Utils
import org.musicpimp.Utils.urlEncode
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
    private val coversDir = app.applicationContext.cacheDir.resolve("covers")
    private val coversStream = MutableLiveData<Bitmap?>()
    val covers: LiveData<Bitmap?> = coversStream

    fun onPlay() {
        main.playerSocket?.resume()
    }

    fun onPause() {
        main.playerSocket?.stop()
    }

    fun onNext() {
        main.playerSocket?.next()
    }

    fun onPrevious() {
        main.playerSocket?.prev()
    }

    fun updateCover(track: Track) {
        viewModelScope.launch {
            val bitmap = try {
                fetchCover(track)?.let { file ->
                    BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch cover for '${track.artist} - ${track.album}'.")
                null
            }
            coversStream.postValue(bitmap)
        }
    }

    private suspend fun fetchCover(track: Track): File? {
        val filenameNoExt = Utils.hashString("${track.artist}-${track.album}")
        val destination = coversDir.resolve("$filenameNoExt.jpg")
        return if (destination.exists()) {
            destination
        } else {
            val artistEnc = urlEncode(track.artist.value)
            val albumEnc = urlEncode(track.album.value)
            val url =
                FullUrl("https", "api.musicpimp.org", "/covers?artist=$artistEnc&album=$albumEnc")
            OkClient.default.download(url, destination)?.let { _ ->
                coversStream.postValue(BitmapFactory.decodeFile(destination.absolutePath))
                destination
            }
        }
    }
}
