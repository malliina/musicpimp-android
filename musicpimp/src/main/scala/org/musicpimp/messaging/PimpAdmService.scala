package org.musicpimp.messaging

import android.content.Intent
import com.amazon.device.messaging.ADMMessageHandlerBase
import org.musicpimp.messaging.AdmMessages.{AdmMessage, Registered, RegistrationError, Unregistered}
import org.musicpimp.util.PimpLog
import rx.lang.scala.{Observable, Subject}

/** https://developer.amazon.com/public/apis/engage/device-messaging/tech-docs/04-integrating-your-app-with-adm
  *
  * Other code modules will want to call ADM.startRegister() at will, and get a registration ID back. But no such API
  * is provided by Amazon, instead, this BroadcastReceiver is the only entity that receives IDs (asynchronously). To
  * enable the desired API, this class thus implements the Observer pattern and publishes received registration ID
  * events to AdmEvents. Other code modules may then profit as follows:
  *
  * 1) subscribe to registration events provided by `AdmEvents.events`
  * 2) call ADM.startRegister()
  * 3) wait for an event
  * 4) once an event is received, unsubscribe
  *
  * See [[org.musicpimp.andro.messaging.AmazonMessaging]] for an implementation that provides a
  * [[scala.concurrent.Future]]-based API for registration IDs.
  */
class PimpAdmService extends ADMMessageHandlerBase(classOf[PimpAdmService].getName) with CloudMessageService with PimpLog {
  override def onMessage(intent: Intent): Unit =
    Option(intent.getExtras).fold(warn(s"No extras in ADM intent"))(bundle => onMessage(bundle))

  /**
    * @param id registration ID
    */
  override def onRegistered(id: String): Unit = publishMaybe(id, Registered.apply)

  override def onUnregistered(id: String): Unit = publishMaybe(id, Unregistered.apply)

  override def onRegistrationError(id: String): Unit = publishMaybe(id, RegistrationError.apply)

  private def publishMaybe(idOpt: String, f: String => AdmMessage): Unit =
    Option(idOpt).foreach(realID => AdmEvents.publish(f(realID)))
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
