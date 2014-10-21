package org.musicpimp.network

import com.mle.android.http.{HttpConstants, JsonHttpClient, Protocols}
import org.musicpimp.http.Endpoint

import scala.concurrent.duration.DurationLong

/**
 * @author Michael
 */
class BasicHttpClient2(endpoint: Endpoint)
  extends JsonHttpClient {
  httpClient setTimeout (5 seconds).toMillis.toInt
  addHeaders(HttpConstants.AUTHORIZATION -> endpoint.authValue)

  val baseUrl = s"$scheme://${endpoint.host}:${endpoint.port}"

  private def scheme = if (endpoint.protocol == Protocols.Https) "https" else "http"

  override def transformUri(uri: String): String = baseUrl + uri
}
