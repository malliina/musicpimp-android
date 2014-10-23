package org.musicpimp.pimp

import com.mle.concurrent.Futures
import com.mle.ws.SocketClient
import org.musicpimp.json.JsonStrings.{EVENT, WELCOME}
import play.api.libs.json.JsValue

import scala.concurrent.{Future, Promise}

/**
 * @author Michael
 */
trait WelcomeExpectation extends SocketClient[JsValue] {
  protected val welcomePromise = Promise[Unit]()

  override def connect(): Future[Unit] = {
    Futures.timeoutAfter(connectTimeout, welcomePromise)
    super.connect().flatMap(_ => welcomePromise.future)
  }

  override def onMessage(json: JsValue): Unit = {
    if (isWelcome(json)) {
      welcomePromise.trySuccess()
    }
    super.onMessage(json)
  }

  override def onClose(): Unit = {
    super.onClose()
    failWelcoming(PimpWebSocket.connectionClosed)
  }

  override def onError(t: Exception): Unit = {
    super.onError(t)
    failWelcoming(t)
  }

  def isWelcome(json: JsValue) = (json \ EVENT).asOpt[String].contains(WELCOME)

  def failWelcoming(e: Exception) = welcomePromise tryFailure e

}
