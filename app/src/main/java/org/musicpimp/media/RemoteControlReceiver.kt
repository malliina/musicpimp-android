package org.musicpimp.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import org.musicpimp.PimpComponents
import org.musicpimp.audio.Player

/** Handles playback commands from external devices.
 */
class RemoteControlReceiver(private val comps: PimpComponents) : BroadcastReceiver() {
    companion object {
        private val intentFilter = IntentFilter(Intent.ACTION_MEDIA_BUTTON)

        fun register(comps: PimpComponents) =
            comps.appContext.registerReceiver(RemoteControlReceiver(comps), intentFilter)
    }

    val player: Player get() = comps.player

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MEDIA_BUTTON -> {
                val event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY ->
                        player.resume()
                    KeyEvent.KEYCODE_MEDIA_PAUSE ->
                        player.pause()
                    KeyEvent.KEYCODE_MEDIA_NEXT ->
                        player.next()
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS ->
                        player.prev()
                }
            }

        }
    }
}
