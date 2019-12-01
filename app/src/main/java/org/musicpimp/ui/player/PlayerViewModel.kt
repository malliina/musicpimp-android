package org.musicpimp.ui.player

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
    private val conf = (app as PimpApp).conf
    private val player: Player
        get() = conf.player
    private val coversDir = app.applicationContext.cacheDir.resolve("covers")
    private val coversStream = MutableLiveData<Bitmap?>()
    private val metadataStream = MutableLiveData<MediaMetadataCompat>()

    val covers: LiveData<Bitmap?> = coversStream
    val metadata: LiveData<MediaMetadataCompat> = metadataStream

    fun onPlay() {
        player.resume()
    }

    fun onPause() {
        player.stop()
    }

    fun onNext() {
        player.next()
    }

    fun onPrevious() {
        player.prev()
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
