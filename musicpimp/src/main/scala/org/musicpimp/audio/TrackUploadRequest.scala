package org.musicpimp.audio

import com.malliina.http.FullUrl
import play.api.libs.json.Json

case class TrackUploadRequest(track: String, uri: FullUrl, username: String, password: String)

object TrackUploadRequest {
  implicit val jsonFormat = Json.format[TrackUploadRequest]
}
