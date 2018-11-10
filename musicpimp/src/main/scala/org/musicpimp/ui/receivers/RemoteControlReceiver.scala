package org.musicpimp.ui.receivers

import android.content.{Intent, Context, BroadcastReceiver}
import android.view.KeyEvent
import org.musicpimp.audio.PlayerManager

/** Handles hardware key presses of prev/next/pause/play, if any.
  */
class RemoteControlReceiver extends BroadcastReceiver {
  def player = PlayerManager.active

  def onReceive(context: Context, intent: Intent) {
    intent.getAction match {
      case Intent.ACTION_MEDIA_BUTTON =>
        val event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT).asInstanceOf[KeyEvent]
        event.getKeyCode match {
          case KeyEvent.KEYCODE_MEDIA_PLAY =>
            player.resume()
          case KeyEvent.KEYCODE_MEDIA_PAUSE =>
            player.pause()
          case KeyEvent.KEYCODE_MEDIA_NEXT =>
            player.playNext()
          case KeyEvent.KEYCODE_MEDIA_PREVIOUS =>
            player.playPrevious()
        }
    }
  }
}
