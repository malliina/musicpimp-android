package org.musicpimp.messaging

import android.content.Intent
import com.amazon.device.messaging.ADMMessageHandlerBase
import org.musicpimp.messaging.AdmMessages.{AdmMessage, RegistrationError, Unregistered, Registered}
import org.musicpimp.util.PimpLog
import rx.lang.scala.{Observable, Subject}

/**
 * https://developer.amazon.com/public/apis/engage/device-messaging/tech-docs/04-integrating-your-app-with-adm
 *
 * Other code modules will want to call ADM.startRegister() at will, and get a registration ID back. But no such API
 * is provided by Amazon, instead, this BroadcastReceiver is the only entity that receives IDs (asynchronously). To
 * enable the desired API, this class thus implements the Observer pattern and publishes received registration ID
 * events to AdmEvents. Other code modules may then profit as follows:
 *
 * 1) subscribe to registration events using AdmEvents.addHandler(...)
 * 2) call ADM.startRegister()
 * 3) wait for an event
 * 4) once an event is received, unsubscribe from events using AdmEvents.removeHandler(...)
 *
 * See [[org.musicpimp.andro.messaging.AmazonMessaging]] for an implementation that provides a
 * [[scala.concurrent.Future]]-based API for registration IDs.
 *
 * @author Michael
 */
class PimpAdmService extends ADMMessageHandlerBase(classOf[PimpAdmService].getName) with CloudMessageService with PimpLog {
  override def onMessage(intent: Intent): Unit =
    Option(intent.getExtras).fold(warn(s"No extras in ADM intent"))(bundle => onMessage(bundle))

  /**
   * @param id registration ID
   */
  override def onRegistered(id: String): Unit = AdmEvents.publish(Registered(id)) // serverMessenger.registerId(this, id)

  override def onUnregistered(id: String): Unit = AdmEvents.publish(Unregistered(id)) //serverMessenger.unregisterId(this, id)

  override def onRegistrationError(id: String): Unit = AdmEvents.publish(RegistrationError(id)) //warn(s"ADM registration error for ID: $id")
}

object AdmEvents {
  private val eventsSubject = Subject[AdmMessage]()
  val events: Observable[AdmMessage] = eventsSubject

  def publish(event: AdmMessage): Unit = {
    eventsSubject onNext event
  }
}

object AdmMessages {

  trait AdmMessage

  case class Registered(id: String) extends AdmMessage

  case class Unregistered(id: String) extends AdmMessage

  case class RegistrationError(id: String) extends AdmMessage

}