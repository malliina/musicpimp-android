package org.musicpimp.audio

import play.api.libs.json.Json

case class TrackUploadRequest(track: String, uri: String, username: String, password: String)

object TrackUploadRequest {
  implicit val jsonFormat = Json.format[TrackUploadRequest]
}
