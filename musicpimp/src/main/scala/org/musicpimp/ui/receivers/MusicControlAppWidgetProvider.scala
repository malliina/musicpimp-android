package org.musicpimp.ui.receivers

import MusicControlAppWidgetProvider._
import android.app.PendingIntent
import android.appwidget.{AppWidgetManager, AppWidgetProvider}
import android.content.{ComponentName, Intent, Context}
import android.widget.RemoteViews
import org.musicpimp.R
import org.musicpimp.audio.{Track, PlayerManager}
import org.musicpimp.ui.AppWidgetTrackListener

class MusicControlAppWidgetProvider extends AppWidgetProvider {
  def updateAppWidget(ctx: Context, trackOpt: Option[Track], playing: Boolean) {
    val views = registerClickHandlers(ctx)
    val (title, artist) = trackOpt map (t => (t.title, t.artist)) getOrElse("No track", "")
    views.setTextViewText(R.id.widget_title, title)
    views.setTextViewText(R.id.widget_artist, artist)
    val playPauseIcon =
      if (playing) android.R.drawable.ic_media_pause
      else R.drawable.ic_media_play
    views.setImageViewResource(R.id.play_button, playPauseIcon)
    //    info("Updating track status of app widget...")
    AppWidgetManager.getInstance(ctx).updateAppWidget(new ComponentName(ctx.getPackageName, classOf[MusicControlAppWidgetProvider].getName), views)
  }

  def registerClickHandlers(ctx: Context): RemoteViews = {
    // Get the layout for the App Widget and attach on-click listeners
    val views = new RemoteViews(ctx.getPackageName, R.layout.widget)
    initAppWidgetOnClick(APP_WIDGET_PLAY, R.id.play_button)
    initAppWidgetOnClick(APP_WIDGET_SKIP, R.id.skip_button)

    /**
     * Adds a click handler to `buttonRes` that sends an intent with name `intentName` when clicked.
     * To intercept the intent, define an intent-filter with the same name as `intentName` in
     * AndroidManifest.xml under the desired intent receiver, then handle the intent in `onReceive`
     * of the receiver.
     *
     * @param intentName name of intent
     * @param buttonRes XML resource if of app widget button
     */
    def initAppWidgetOnClick(intentName: String, buttonRes: Int): Unit = {
      val intent = new Intent(intentName)
      val pending = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
      views.setOnClickPendingIntent(buttonRes, pending)
    }
    views
  }

  // seems this is not called when the app widget is initially `onEnabled`, but when subsequent app widgets are added
  override def onUpdate(ctx: Context, appWidgetManager: AppWidgetManager, appWidgetIds: Array[Int]): Unit = {
    //    info("app widget: onUpdate")
    val N = appWidgetIds.length
    if (N == 1) {
      // On android 4, onEnabled is apparently never called, only onUpdate. So we hope that if there's only one widget
      // to update, it's the first one added, therefore we add listeners.
      registerAndUpdateWidgets(ctx)
    }
    // Perform this loop procedure for each App Widget that belongs to this provider
    for (i <- 0 until N) {

      val appWidgetId = appWidgetIds(i)

      val views = registerClickHandlers(ctx)
      // Tell the AppWidgetManager to perform an update on the current app widget
      appWidgetManager.updateAppWidget(appWidgetId, views)
      // update with the current track
      updateAppWidget(ctx, player.currentTrack, player.playing)
    }
  }

  override def onEnabled(ctx: Context) {
    super.onEnabled(ctx)
    //    info(s"app widget: onEnabled")
    registerAndUpdateWidgets(ctx)
  }

  def registerAndUpdateWidgets(ctx: Context) {
    updateAppWidget(ctx, player.currentTrack, player.playing)
    AppWidgetTrackListener.unregisterPlayerEventsListener()
    AppWidgetTrackListener.registerPlayerEventsListener()
  }

  override def onDisabled(context: Context) {
    super.onDisabled(context)
    AppWidgetTrackListener.unregisterPlayerEventsListener()
  }

  def player = PlayerManager.active

  override def onReceive(context: Context, intent: Intent) {
    //    info(s"app widget: onReceive ${intent.getAction}")
    super.onReceive(context, intent)

    /**
     * Intercepts an intent launched when the user clicks an app widget button of this app.
     */
    intent.getAction match {
      case APP_WIDGET_PLAY =>
        player.playOrPause()
      case APP_WIDGET_SKIP =>
        player.playNext()
      case APP_WIDGET_UPDATE =>
        updateAppWidget(context, player.currentTrack, player.playing)
      case _ => ()
    }
  }
}

object MusicControlAppWidgetProvider {
  // actions with the same string values are defined in AndroidManifest.xml
  val APP_WIDGET_PLAY = "org.musicpimp.widget.PLAY_PAUSE"
  val APP_WIDGET_SKIP = "org.musicpimp.widget.SKIP"
  val APP_WIDGET_UPDATE = "org.musicpimp.widget.UPDATE_TRACK"
}
