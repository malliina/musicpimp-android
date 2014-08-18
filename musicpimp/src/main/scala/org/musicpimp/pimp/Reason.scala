package org.musicpimp.pimp

import play.api.libs.json.Json

/**
 *
 * @author mle
 */
case class Reason(reason: String)

object Reason {
  implicit val jsonFormat = Json.format[Reason]
}
