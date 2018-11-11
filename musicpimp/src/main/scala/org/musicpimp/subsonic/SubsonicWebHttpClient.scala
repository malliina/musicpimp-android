package org.musicpimp.subsonic

import com.loopj.android.http.TextHttpResponseHandler
import com.malliina.android.http.HttpConstants.{ACCEPT, JSON}
import com.malliina.android.http.{BasicHttpClient, HttpResponse}
import org.musicpimp.http.Endpoint

import scala.concurrent.Promise

class SubsonicWebHttpClient(endpoint: Endpoint) extends BasicHttpClient(endpoint) {
  addHeaders(ACCEPT -> JSON)

  /**
   * Subsonic may return 200 OK HTTP responses even though a request fails. To determine whether a
   * request was successful, we need to inspect the "status" JSON key of each response.
   * Thus this handler only completes the HttpResponse promise successfully if the HTTP status code
   * is fine and the "status" key in the response is not "failed". If the "status" key is
   * "failed", the promise is failed with a [[org.musicpimp.exceptions.SubsonicHttpException]].
   *
   * @param promise promise to complete when a response has been received
   * @return a custom HTTP response handler for Subsonic
   */
  override def textResponseHandler(promise: Promise[HttpResponse]): TextHttpResponseHandler =
    new SubsonicResponseHandler(promise)
}
