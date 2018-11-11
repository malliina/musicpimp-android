package org.musicpimp.network

import com.malliina.android.http.{HttpConstants, HttpResponse, HttpUtil, JsonHttpClient}
import org.musicpimp.http.Endpoint

import scala.concurrent.Future

class PingHttpClient extends JsonHttpClient {
  def setBasicAuth(username: String, password: String): Unit =
    addHeaders(
      HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue(username, password)
    )

  def ping(endpoint: Endpoint, pingResource: String = "/ping"): Future[HttpResponse] =
    get((endpoint.httpBaseUri / pingResource).url)
}

object UtilHttpClient extends PingHttpClient
