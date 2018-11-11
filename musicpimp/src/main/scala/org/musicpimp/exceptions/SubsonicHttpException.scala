package org.musicpimp.exceptions

import com.malliina.android.exceptions.ExplainedHttpException
import org.musicpimp.subsonic.SubsonicJsonReaders._
import play.api.libs.json.Json

import scala.util.Try

class SubsonicHttpException(content: Option[String]) extends ExplainedHttpException(content) {
  val reasonOpt = for {
    c <- content
    json <- Try(Json parse c).toOption
    message <- (json \ SUBSONIC_RESPONSE \ ERROR \ MESSAGE).asOpt[String]
  } yield message
  val reason = "An error occurred while connecting to Subsonic. " + reasonOpt.getOrElse("")
}
