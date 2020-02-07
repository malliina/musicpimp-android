package org.musicpimp.media

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList
import org.musicpimp.Duration
import org.musicpimp.Playstate
import org.musicpimp.Track
import org.musicpimp.audio.Player
import org.musicpimp.millis
import org.musicpimp.ui.player.CoverService
import timber.log.Timber

/** Media player that delegates calls by sending intents to the background audio
 * <code>Service</code>. The Service in turn notifies this player of playback updates.
 */
class LocalPlayer(private val appContext: Context, private val covers: CoverService) : Player {
    private val currentTracks: MutableList<Track> = mutableListOf()
    // The player may have a track which is not in the playlist
    var playerTrack: Track? = null
    private val playerTracks = MutableLiveData<Track>()
    private val playlist = MutableLiveData<List<Track>>().apply {
        value = emptyList()
    }
    private val indices = MutableLiveData<Int?>().apply {
        value = null
    }
    private val playstates = MutableLiveData<Playstate>().apply {
        value = Playstate.NoMedia
    }
    private val currentPosition = MutableLiveData<Duration>().apply {
        value = 0.millis
    }
    val tracks: LiveData<Track> = playerTracks
    val list: LiveData<List<Track>> = playlist
    val index: LiveData<Int?> = indices
    val states: LiveData<Playstate> = playstates
    val position: LiveData<Duration> = currentPosition

    private var currentState: Playstate = Playstate.NoMedia

    private val currentIndex: Int?
        get() = index.value

//    val dummy = states.observeForever { s ->
//        playerTrack?.let { t ->
//            updateNotification(t, s)
//        }
//    }

    override fun play(track: Track) {
        currentTracks.clear()
        addAll(listOf(track))
        indices.postValue(0)
        playerTrack = track
        startIntent(PimpMediaService.RESTART_ACTION)
    }

    override fun add(track: Track) {
        addAll(listOf(track))
    }

    override fun addAll(tracks: List<Track>) {
        currentTracks.addAll(tracks)
        playlist.postValue(currentTracks.toImmutableList())
    }

    override fun resume() {
        startIntent(PimpMediaService.RESUME_ACTION)
    }

    override fun next() {
        startIntent(PimpMediaService.NEXT_ACTION)
    }

    override fun prev() {
        startIntent(PimpMediaService.PREV_ACTION)
    }

    override fun skip(idx: Int) {
        indices.postValue(idx)
        startIntent(PimpMediaService.PLAY_PLAYLIST)
    }

    override fun remove(idx: Int) {
        currentTracks.removeAt(idx)
        currentIndex?.let { i ->
            if (idx <= i && i >= 0) {
                indices.postValue(i - 1)
            }
        }
        playlist.postValue(currentTracks.toImmutableList())
    }

    override fun seek(to: Duration) {
        startIntent(PimpMediaService.SEEK_ACTION) { i ->
            i.putExtra(PimpMediaService.POSITION_EXTRA, to)
        }
    }

    override fun stop() {
        startIntent(PimpMediaService.PAUSE_ACTION)
        onState(Playstate.Stopped)
    }

    override fun pause() {
        startIntent(PimpMediaService.PAUSE_ACTION)
        playstates.postValue(Playstate.Paused)
    }

    fun toNext(): Track? {
        val ret = nextTrack()
        ret?.let { _ -> currentIndex?.let { i -> indices.postValue(i + 1) } }
        return ret
    }

    fun toPrev(): Track? {
        val ret = prevTrack()
        ret?.let { _ -> currentIndex?.let { i -> indices.postValue(if (i > 0) i - 1 else 0) } }
        return ret
    }

    private fun nextTrack(): Track? = withIndex { i -> i + 1 }

    private fun prevTrack(): Track? = withIndex { i -> if (i > 0) i - 1 else 0 }

    fun onTrack(track: Track?) {
        playerTrack = track
        playerTracks.postValue(track)
    }

//    private fun updateNotification(track: Track, playstate: Playstate) {
//        GlobalScope.launch {
//            withContext(Dispatchers.IO) {
//                if (playstate == Playstate.Stopped) {
//                    notifications.cancel()
//                } else {
//                    Timber.i("Updating notification with $playstate")
//                    val bitmap = covers.cover(track)
//                    notifications.displayTrackNotification(
//                        track,
//                        playstate == Playstate.Playing,
//                        bitmap
//                    )
//                }
//            }
//        }
//    }

    fun onState(state: Playstate) {
        currentState = state
        playstates.postValue(state)
    }

    fun onPosition(pos: Duration) {
        currentPosition.postValue(pos)
    }

    private fun withIndex(f: (old: Int) -> Int): Track? =
        currentIndex?.let { idx -> trackAt(f(idx)) }

    private fun trackAt(index: Int): Track? =
        if (index < currentTracks.size && index >= 0) currentTracks[index] else null

    private fun startIntent(action: String) {
        startIntent(action) { i -> i }
    }

    private fun startIntent(action: String, prep: (i: Intent) -> Intent) {
        val context = appContext
        val intent = playbackIntent(context, action)
        context.startService(prep(intent))
    }

    private fun playbackIntent(ctx: Context, action: String): Intent {
        val intent = Intent(ctx, PimpMediaService::class.java)
        intent.action = action
        return intent
    }
}
