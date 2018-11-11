package org.musicpimp.pimp

import com.malliina.android.http.{HttpConstants, MySslSocketFactory}
import com.malliina.ws.JsonWebSocketClient
import org.musicpimp.http.Endpoint
import play.api.libs.json.JsValue

class PimpWebSocket(endpoint: Endpoint, wsResource: String, handler: JsValue => Unit)
  extends JsonWebSocketClient(
    endpoint.wsBaseUri / wsResource,
    Some(MySslSocketFactory.trustAllSslContext()),
    Map(
      HttpConstants.ACCEPT -> PimpConstants.JSONv18,
      HttpConstants.AUTHORIZATION -> endpoint.authValue)
    )
    with WelcomeExpectation {

  override def onMessage(json: JsValue): Unit = handler(json)
}

object PimpWebSocket {
  val connectionClosed = new Exception("Connection closed.")
}
