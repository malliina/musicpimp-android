package org.musicpimp.exceptions

import com.mle.android.exceptions.ExplainedHttpException
import org.musicpimp.subsonic.SubsonicJsonReaders._
import play.api.libs.json.Json

/**
 *
 * @author mle
 */
class SubsonicHttpException(content: String) extends ExplainedHttpException(Some(content)) {
  val reasonOpt = (Json.parse(content) \ SUBSONIC_RESPONSE \ ERROR \ MESSAGE).asOpt[String]
  val reason = "An error occurred while connecting to Subsonic. " + reasonOpt.getOrElse("")
}
