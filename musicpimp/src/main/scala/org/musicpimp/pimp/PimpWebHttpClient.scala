package org.musicpimp.pimp

import android.content.Context
import com.malliina.android.http.HttpConstants.ACCEPT
import com.malliina.android.http.HttpResponse
import cz.msebera.android.httpclient.client.HttpResponseException
import org.musicpimp.exceptions.PimpHttpException
import org.musicpimp.http.Endpoint
import org.musicpimp.network.BasicHttpClient2
import play.api.libs.json.{Json, Writes}

import scala.concurrent.Future

class PimpWebHttpClient(val endpoint: Endpoint) extends BasicHttpClient2(endpoint) {
  addHeaders(ACCEPT -> PimpConstants.JSONv18)

  /** Analyzes what went wrong with a failed HTTP request and returns a more appropriate exception.
    *
    * If the request was authorized, parses the response content as JSON and looks for a reason key, wrapping `t`
    * and the reason in a [[org.musicpimp.exceptions.PimpHttpException]] if found.
    *
    * @param t       the failure
    * @param content any content
    * @return a more refined exception, or `t` if refinement failed
    */
  override def handleFailure(t: Throwable, content: Option[String]): Throwable = {
    val pimpFailureRefiner: PartialFunction[Throwable, Throwable] = {
      case hre: HttpResponseException => new PimpHttpException(content, hre)
    }
    val handler = httpFailureRefiner orElse pimpFailureRefiner orElse passThroughHandler
    handler.applyOrElse(t, (fail: Throwable) => fail)
  }

  def postBody[T](ctx: Context, uri: String, body: T)(implicit writes: Writes[T]): Future[HttpResponse] = {
    val json = Json toJson body
    post(ctx, uri, json)
  }
}
