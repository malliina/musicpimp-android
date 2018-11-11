package org.musicpimp.ui.activities

import android.app.Activity
import org.musicpimp.audio.PlayerEventListening

trait PlayerListeningActivity extends Activity with PlayerEventListening {
  override def onResume(): Unit = {
    super.onResume()
    subscribeToPlayerEvents()
  }

  override def onPause(): Unit = {
    unsubscribeFromPlayerEvents()
    super.onPause()
  }
}
