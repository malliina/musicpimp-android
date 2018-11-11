package org.musicpimp.beam

import play.api.libs.json.Json

case class BeamCode(host: String, port: Int, user: String)

object BeamCode {
  implicit val jsonReader = Json.format[BeamCode]
  val default = BeamCode("beam.musicpimp.org", 80, "Go to beam.musicpimp.org")
}
