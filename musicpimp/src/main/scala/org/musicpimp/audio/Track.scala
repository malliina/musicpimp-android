package org.musicpimp.audio

import android.net.Uri
import com.malliina.json.JsonFormats
import org.musicpimp.http.Endpoint
import play.api.libs.json.{JsValue, Json, Writes}

import scala.concurrent.duration.Duration

/**
  * @param source the URI to supply to a media player to begin playback
  */
case class Track(id: String,
                 title: String,
                 album: String,
                 artist: String,
                 path: String,
                 duration: Duration,
                 size: Long,
                 source: Uri,
                 username: String,
                 password: String,
                 cloudID: Option[String]) extends MusicItem {
  lazy val authValue = Endpoint.header(cloudID, username, password)
}

object TrackHelp {
  implicit val dur = JsonFormats.duration

  import org.musicpimp.json.JsonStrings._

  object json extends Writes[Track] {
    override def writes(o: Track): JsValue = Json.obj(
      ID -> o.id,
      TITLE -> o.title,
      ALBUM -> o.album,
      ARTIST -> o.artist,
      PATH -> o.path,
      DURATION -> Json.toJson(o.duration),
      SIZE -> o.size
    )
  }

}
