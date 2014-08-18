package org.musicpimp.messaging

import android.app.{IntentService, PendingIntent, NotificationManager}
import android.content.{Intent, Context}
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import org.musicpimp.R
import org.musicpimp.andro.util.Implicits.RichBundle
import org.musicpimp.ui.activities.StopAlarm
import org.musicpimp.util.PimpLog

/**
 * @author Michael
 */
trait CloudMessageService extends IntentService with PimpLog {
  val NOTIFICATION_ID = 1
  val key = StopAlarm.tagKey

  def onMessage(extras: Bundle): Unit = {
    (extras findString key).fold(warn(s"No key: $key in cloud message: $extras"))(displayNotification)
  }

  private def displayNotification(endpointID: String): Unit = {
    val mgr = this.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
    val intent = new Intent(this, classOf[StopAlarm])
    intent.putExtra(key, endpointID)
    val contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    val builder = new NotificationCompat.Builder(this)
      .setSmallIcon(R.drawable.ic_launcher)
      .setContentTitle("Playing")
      .setStyle(new NotificationCompat.BigTextStyle().bigText("Tap to stop"))
    builder setAutoCancel true
    builder setContentIntent contentIntent
    mgr.notify(NOTIFICATION_ID, builder.build())
  }
}
