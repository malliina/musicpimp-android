package org.musicpimp.ui

import android.content.Intent
import org.musicpimp.PimpApp
import org.musicpimp.audio.{Track, PlayStates, TrackListener}
import org.musicpimp.ui.receivers.MusicControlAppWidgetProvider

class AppWidgetTrackListener extends TrackListener {
  def onTrackChanged(trackOpt: Option[Track]): Unit = {
    broadcastUpdateIntent()
  }

  def onPlayStateChanged(state: PlayStates.PlayState): Unit = {
    broadcastUpdateIntent()
  }

  def broadcastUpdateIntent() {
    val intent = new Intent(MusicControlAppWidgetProvider.APP_WIDGET_UPDATE)
    PimpApp.context.sendBroadcast(intent)
    //    info(s"Sent intent ${MusicControlAppWidgetProvider.APP_WIDGET_UPDATE}")
  }
}

object AppWidgetTrackListener extends AppWidgetTrackListener
