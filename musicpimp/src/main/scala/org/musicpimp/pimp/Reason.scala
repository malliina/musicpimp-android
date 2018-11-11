package org.musicpimp.pimp

import play.api.libs.json.Json

case class Reason(reason: String)

object Reason {
  implicit val jsonFormat = Json.format[Reason]
}
