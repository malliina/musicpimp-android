package org.musicpimp.subsonic

import com.loopj.android.http.TextHttpResponseHandler
import com.malliina.android.http.HttpResponse
import cz.msebera.android.httpclient.Header
import org.musicpimp.exceptions.SubsonicHttpException
import org.musicpimp.subsonic.SubsonicJsonReaders.{FAILED, STATUS, SUBSONIC_RESPONSE}
import play.api.libs.json.Json

import scala.concurrent.Promise
import scala.util.Try

class SubsonicResponseHandler(promise: Promise[HttpResponse]) extends TextHttpResponseHandler {
  override def onSuccess(statusCode: Int, headers: Array[Header], responseString: String): Unit = {
    val content = Option(responseString)
    val isFailure: Boolean = (for {
      c <- content
      json <- Try(Json parse c).toOption
      status <- (json \ SUBSONIC_RESPONSE \ STATUS).asOpt[String]
    } yield status) contains FAILED
    if (isFailure) {
      promise failure new SubsonicHttpException(content)
    } else {
      promise success HttpResponse(statusCode, content)
    }
  }

  override def onFailure(statusCode: Int, headers: Array[Header], responseString: String, throwable: Throwable): Unit = {
    //    info(s"Failed response: $responseString")
    promise failure Option(throwable).getOrElse(new SubsonicHttpException(Option(responseString)))
  }
}
