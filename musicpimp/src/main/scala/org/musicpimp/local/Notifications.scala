package org.musicpimp.local

import android.app.{NotificationManager, PendingIntent}
import android.content.{Context, Intent}
import android.graphics.BitmapFactory
import android.support.v4.app.{TaskStackBuilder, NotificationCompat}
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.R
import org.musicpimp.audio.Track
import org.musicpimp.network.DiscoGs
import org.musicpimp.ui.activities.MainActivity
import scala.util.{Failure, Success}

class Notifications(ctx: Context) {
  private def notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

  /** http://developer.android.com/guide/topics/ui/notifiers/notifications.html#Managing
    *
    * @param track track to show in notification
    */
  def displayTrackNotification(track: Track, playing: Boolean): Unit = {
    DiscoGs.client.cover(track.artist, track.album)
      .map(file => BitmapFactory.decodeFile(file.getAbsolutePath))
      .onComplete {
        case Success(bitmap) =>
          displayNotification(track, _.setLargeIcon(bitmap), playing)
        case Failure(t) =>
          //        warn(s"Cover download failed: ${t.getMessage}", t)
          displayNotification(track, b => b, playing)
      }
  }

  private def displayNotification(track: Track, f: NotificationCompat.Builder => NotificationCompat.Builder, playing: Boolean): Unit = {
    val contentText = if (playing) "Playing" else "Paused"
    val builder = f(new NotificationCompat.Builder(ctx)
      .setSmallIcon(R.drawable.guitar_light)
      .setContentTitle(track.title)
      .setContentText(contentText))
    val intent = new Intent(ctx.getApplicationContext, classOf[MainActivity])
    // Ensures that when the notification is clicked and the user is taken to the app,
    // clicking back leads out of the app.
    val stackBuilder = TaskStackBuilder.create(ctx)
    stackBuilder addParentStack classOf[MainActivity]
    stackBuilder addNextIntent intent
    // 0 is requestCode, whatever the fuck that is
    val pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    builder setContentIntent pendingIntent

    def servicePendingIntent(action: String) = {
      val intent = new Intent(ctx, classOf[MediaService]).setAction(action)
      PendingIntent.getService(ctx, 0, intent, 0)
    }

    def addAction(action: String, drawable: Int, title: CharSequence): Unit = {
      val pi = servicePendingIntent(action)
      builder.addAction(drawable, title, pi)
    }

    if (playing) {
      addAction(MediaService.PAUSE_ACTION, R.drawable.ic_media_pause, "Pause")
    } else {
      addAction(MediaService.RESUME_ACTION, R.drawable.ic_media_play, "Play")
    }
    addAction(MediaService.NEXT_ACTION, R.drawable.ic_media_next, "Next")
    addAction(MediaService.CLOSE_ACTION, R.drawable.abc_ic_clear_mtrl_alpha, "Close")
    // clicking "clear all" will have no effect
    builder.setOngoing(true)
    // the notification id allows us to update the notification later on
    notificationManager.notify(Notifications.PIMP_NOTIFICATION_ID, builder.build())
  }

  def cancel(): Unit =
    notificationManager.cancel(Notifications.PIMP_NOTIFICATION_ID)
}

object Notifications {
  val PIMP_NOTIFICATION_ID = 666
}
