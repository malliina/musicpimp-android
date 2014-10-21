package org.musicpimp.pimp

import com.mle.android.http.{HttpConstants, MySslSocketFactory}
import com.mle.ws.JsonWebSocketClient
import org.musicpimp.http.Endpoint
import play.api.libs.json.JsValue

/**
 *
 * @author mle
 */
class PimpWebSocket(endpoint: Endpoint, wsResource: String, handler: JsValue => Unit)
  extends JsonWebSocketClient(
    endpoint.wsBaseUri + wsResource,
    Some(MySslSocketFactory.trustAllSslContext()),
    HttpConstants.ACCEPT -> PimpConstants.JSONv18, HttpConstants.AUTHORIZATION -> endpoint.authValue) {

  override def onMessage(json: JsValue): Unit = handler(json)
}
