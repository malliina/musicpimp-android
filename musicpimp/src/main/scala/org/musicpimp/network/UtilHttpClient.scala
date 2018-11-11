package org.musicpimp.network

import android.util.Base64
import com.malliina.android.http.{HttpConstants, HttpResponse, JsonHttpClient}
import org.musicpimp.http.Endpoint

import scala.concurrent.Future

class PingHttpClient extends JsonHttpClient {
  def setBasicAuth(username: String, password: String): Unit = {
    val unencoded = username + ":" + password
    val encoded = Base64.encodeToString(unencoded.getBytes("UTF-8"), Base64.NO_WRAP).trim
    addHeaders(
      HttpConstants.AUTHORIZATION -> s"Basic $encoded"
    )
  }


  def ping(endpoint: Endpoint, pingResource: String = "/ping"): Future[HttpResponse] =
    get((endpoint.httpBaseUri / pingResource).url)
}

object UtilHttpClient extends PingHttpClient
