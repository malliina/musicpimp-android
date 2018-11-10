package org.musicpimp.andro.messaging

import android.app.Activity
import android.content.Context
import java.io.Closeable
import scala.concurrent.Future

trait IMessagingUtils extends Closeable {
  /**
   * Obtains a registration ID from the app store's cloud service and registers the ID with the app publisher's
   * messaging server. The messaging server uses the obtained registration ID as a device address when sending messages.
   *
   * The returned [[Future]] contains the registration ID, or fails with a [[MessagingException]] if registration fails.
   *
   * @param activity an activity, because the implementation may opt to present a dialog
   * @return the registration ID
   */
  def tryRegister(activity: Activity): Future[String]

  /**
   * Unregisters this device both from the app store's cloud service and the app publisher's messaging server.
   *
   * Once the returned [[Future]] completes successfully, this device will no longer receive push messages.
   *
   * @param ctx context
   * @return the ID which was successfully unregistered, or a failed [[Future]] otherwise
   */
  def unregister(ctx: Context): Future[String]

  /**
   * Registers this device with the app publisher's messaging server.
   *
   * @param id the registration ID of this device (obtained from the app store's messaging service)
   * @return the registration ID, if registration succeeded, otherwise a failed [[Future]]
   */
  protected def registerId(ctx: Context, id: String): Future[String]

  /**
   * Unregisters this device with the app publisher's messaging server.
   *
   * @param ctx context
   * @param id registration id
   * @return the registration id once unregistration is complete, or a failed [[Future]]
   * @see `registerId(Context,String)`
   */
  def unregisterId(ctx: Context, id: String): Future[String]

  def isRegistered(ctx: Context): Future[Boolean]

  /**
   * Closes any resources, most likely the HTTP client that speaks with the app publisher's server.
   */
  def close(): Unit
}
