package org.musicpimp.network

import com.mle.android.http.{JsonHttpClient, HttpUtil, HttpConstants, HttpResponse}
import org.musicpimp.http.Endpoint
import scala.concurrent.Future

/**
 *
 * @author mle
 */
class PingHttpClient extends JsonHttpClient {
  def setBasicAuth(username: String, password: String) =
    addHeaders(
      HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue(username, password)
    )

  def ping(endpoint: Endpoint, pingResource: String = "/ping"): Future[HttpResponse] =
    get(endpoint.httpBaseUri + pingResource)
}

object UtilHttpClient extends PingHttpClient