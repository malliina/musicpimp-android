package org.musicpimp.andro.messaging

import android.app.Activity
import android.content.Context
import com.amazon.device.messaging.ADM
import com.malliina.android.exceptions.AndroidException
import com.malliina.concurrent.ExecutionContexts.cached
import org.musicpimp.messaging.AdmEvents
import org.musicpimp.messaging.AdmMessages.{AdmMessage, Registered, RegistrationError, Unregistered}

import scala.concurrent.{Future, Promise}

trait AmazonMessaging extends IMessagingUtils {
  def serverMessenger: JsonMessagingUtils

  override def tryRegister(activity: Activity): Future[String] =
    registerADM(activity).flatMap(registerId(activity, _))

  override def unregister(ctx: Context): Future[String] =
    unregisterADM(ctx).flatMap(unregisterId(ctx, _))

  def registerId(ctx: Context, id: String): Future[String] = serverMessenger.registerId(ctx, id)

  def unregisterId(ctx: Context, id: String): Future[String] = serverMessenger.unregisterId(ctx, id)

  override def isRegistered(ctx: Context): Future[Boolean] = Future.successful(new ADM(ctx).getRegistrationId != null)

  def registerADM(ctx: Context): Future[String] = {
    val adm = new ADM(ctx)
    Option(adm.getRegistrationId).fold(startRegistration(ctx))(id => Future.successful(id))
  }

  private def unregisterADM(ctx: Context) = withRegistration(ctx, _.startUnregister())

  private def startRegistration(ctx: Context): Future[String] = withRegistration(ctx, _.startRegister())

  private def withRegistration(ctx: Context, f: ADM => Unit): Future[String] = {
    val p = Promise[String]()
    val subscription = AdmEvents.events.subscribe(msg => completePromise(p, msg))
    val adm = new ADM(ctx)
    f(adm)
    val ret = p.future
    ret.onComplete(_ => subscription.unsubscribe())
    ret
  }

  //  private def toFuture[T](obs: Observable[T]): Future[T] = {
  //    val p = Promise[T]()
  //    val sub = obs.head.subscribe(msg => p trySuccess msg)
  //    val fut = p.future
  //    fut.onComplete(_ => sub.unsubscribe())
  //    fut
  //  }

  private def completePromise(p: Promise[String], event: AdmMessage) = event match {
    case Registered(id) => p trySuccess id
    case Unregistered(id) => p trySuccess id
    case RegistrationError(id) => p tryFailure new AdmException(id)
  }

  override def close(): Unit = {
    serverMessenger.client.close()
  }
}

class AdmException(val id: String) extends AndroidException("ADM failure")
