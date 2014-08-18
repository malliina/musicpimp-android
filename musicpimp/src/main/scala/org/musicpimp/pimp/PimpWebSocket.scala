package org.musicpimp.pimp

import com.mle.android.http.HttpConstants
import com.mle.android.websockets.JsonWebSocketClient
import org.musicpimp.http.Endpoint
import play.api.libs.json.JsValue

/**
 *
 * @author mle
 */
class PimpWebSocket(endpoint: Endpoint, wsResource: String, handler: JsValue => Unit)
  extends JsonWebSocketClient(
    endpoint.wsBaseUri + wsResource,
    endpoint.username,
    endpoint.password,
    HttpConstants.ACCEPT -> PimpConstants.JSONv18) {

  override def onMessage(json: JsValue): Unit = handler(json)
}
