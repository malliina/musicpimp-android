package org.musicpimp.exceptions

import com.fasterxml.jackson.core.JsonParseException
import com.malliina.android.exceptions.ExplainedHttpException
import cz.msebera.android.httpclient.client.HttpResponseException
import org.musicpimp.pimp.Reason
import org.musicpimp.util.PimpLog
import play.api.libs.json.Json

class PimpHttpException(contentOpt: Option[String], cause: HttpResponseException)
  extends ExplainedHttpException(contentOpt, cause) with PimpLog {
  val reasonOpt =
    contentOpt.flatMap { content =>
      try {
        Json.parse(content).asOpt[Reason].map(_.reason)
      } catch {
        case jpe: JsonParseException =>
          //        warn(s"Unable to parse HTTP response. Expected JSON, got something else:\n$content", jpe)
          Some("The response format was not JSON.")
        case _: Exception =>
          Some("The response could not be read.")
      }
    }

  val reason = reasonOpt getOrElse "An error occurred while connecting to the MusicPimp server."
}
