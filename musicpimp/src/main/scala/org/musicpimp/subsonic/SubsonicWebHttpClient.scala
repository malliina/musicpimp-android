package org.musicpimp.subsonic

import com.loopj.android.http.{TextHttpResponseHandler, AsyncHttpResponseHandler}
import com.mle.android.http.HttpConstants.{ACCEPT, JSON}
import com.mle.android.http.{BasicHttpClient, HttpResponse}
import org.apache.http.Header
import org.musicpimp.exceptions.SubsonicHttpException
import org.musicpimp.http.Endpoint
import play.api.libs.json.Json
import scala.concurrent.Promise

/**
 *
 * @author mle
 */
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
    new TextHttpResponseHandler() {
      override def onSuccess(statusCode: Int, headers: Array[Header], responseString: String): Unit = {
        val isFailure = (Json.parse(responseString) \ SubsonicJsonReaders.SUBSONIC_RESPONSE \ SubsonicJsonReaders.STATUS).asOpt[String].contains("failed")
        if (isFailure) {
          promise failure new SubsonicHttpException(responseString)
        } else {
          promise success HttpResponse(statusCode, Option(responseString))
        }
      }

      override def onFailure(statusCode: Int, headers: Array[Header], responseString: String, throwable: Throwable): Unit = {
        promise failure new SubsonicHttpException(responseString)
      }
    }
//  {
//      override def onSuccess(statusCode: Int, content: String): Unit = {
//        val isFailure = (Json.parse(content) \ SubsonicJsonReaders.SUBSONIC_RESPONSE \ SubsonicJsonReaders.STATUS).asOpt[String].exists(_ == "failed")
//        if (isFailure) {
//          promise failure new SubsonicHttpException(content)
//        } else {
//          promise success HttpResponse(statusCode, Option(content))
//        }
//      }
//
//      override def onFailure(t: Throwable, content: String): Unit = {
//        promise failure handleFailure(t, Option(content))
//      }
//    }
}
