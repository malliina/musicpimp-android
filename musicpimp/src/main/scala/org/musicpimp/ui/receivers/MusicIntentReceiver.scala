package org.musicpimp.ui.receivers

import android.content.{Intent, Context, BroadcastReceiver}
import android.media.AudioManager
import org.musicpimp.audio.PlayerManager

/**
 *
 * @author mle
 */
class MusicIntentReceiver extends BroadcastReceiver {
  def localPlayer = PlayerManager.localPlayer

  def onReceive(context: Context, intent: Intent) {
    /**
     *
     * http://developer.android.com/training/managing-audio/audio-output.html
     *
     * When a headset is unplugged, or a Bluetooth device disconnected,
     * the audio stream automatically reroutes to the built in speaker.
     * The system broadcasts an ACTION_AUDIO_BECOMING_NOISY intent when this happens.
     */
    if (intent.getAction == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
      // Android docs recommend pausing playback. I do not agree.
      localPlayer.pause()
    }
  }
}