package org.musicpimp.andro.messaging

import android.app.IntentService
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.WakefulBroadcastReceiver
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.mle.android.util.UtilLog

/**
 * http://developer.android.com/google/gcm/client.html
 *
 * @author mle
 */
abstract class GcmIntentService extends IntentService(classOf[GcmIntentService].getName) with UtilLog {
  override protected def onHandleIntent(intent: Intent): Unit = {
    Option(intent.getExtras)
      .filter(b => !b.isEmpty)
      .fold(warn(s"No extras in intent: $intent"))(extras => onExtras(extras, intent))
    WakefulBroadcastReceiver.completeWakefulIntent(intent)
  }

  def onExtras(extras: Bundle, intent: Intent): Unit = {
    val gcm = GoogleCloudMessaging.getInstance(this)
    val messageType = gcm getMessageType intent
    import GoogleCloudMessaging._
    messageType match {
      case MESSAGE_TYPE_SEND_ERROR =>
        warn(s"GCM send error: $extras")
      case MESSAGE_TYPE_DELETED =>
        warn(s"GCM deleted messages on server: $extras")
      case MESSAGE_TYPE_MESSAGE =>
        info(s"GCM received message: $extras")
        onMessage(extras)
      case _ =>
        warn(s"Unknown GCM message: $extras")
    }
  }

  def onMessage(extras: Bundle): Unit
}
