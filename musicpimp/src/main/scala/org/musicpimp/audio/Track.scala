package org.musicpimp.audio

import android.net.Uri
import com.mle.json.JsonFormats
import org.musicpimp.json.JsonStrings
import play.api.libs.json.{JsValue, Writes, Json}
import scala.concurrent.duration.Duration

/**
 *
 * @author mle
 */
/**
 *
 * @param id
 * @param title
 * @param album
 * @param artist
 * @param path
 * @param duration
 * @param size
 * @param source the URI to supply to a media player to begin playback
 * @param username
 * @param password
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
                 password: String) extends MusicItem

object TrackHelp {
  implicit val dur = JsonFormats.duration

  import JsonStrings._

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