package org.musicpimp.pimp

import android.net.Uri
import org.musicpimp.audio._
import org.musicpimp.http.Endpoint
import org.musicpimp.json.JsonReaders
import org.musicpimp.json.JsonStrings._
import org.musicpimp.json.Readers._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.concurrent.duration._

/**
 *
 * @author mle
 */
class PimpJsonReaders(endpoint: Endpoint) extends JsonReaders(endpoint) {

  def uri(trackId: String): Uri =
    Uri.parse(s"${endpoint.httpBaseUri}/downloads/$trackId")

  implicit val pimpTrackReader: Reads[Track] = (
    (JsPath \ ID).read[String] and
      (JsPath \ TITLE).read[String] and
      (JsPath \ ALBUM).read[String] and
      (JsPath \ ARTIST).read[String] and
      (JsPath \ ID).read[String].map(PimpLibrary.pathFromId) and
      (JsPath \ DURATION).read[Duration] and
      (JsPath \ SIZE).read[Long] and
      (JsPath \ ID).read[String].map(uri) and
      constant(username) and
      constant(password)
    )(Track)

  implicit val folderFormat = Json.format[Folder]

  implicit val dirReader: Reads[Directory] = Json.reads[Directory]

  implicit val statusReader: Reads[StatusEvent] = (
    (__ \ TRACK).read[Track].map(t => if (t.id == "") None else Some(t)) and
      (__ \ STATE).read[PlayStates.PlayState] and
      (__ \ POSITION).read[Duration] and
      (__ \ VOLUME).read[Int] and
      (__ \ MUTE).read[Boolean] and
      (__ \ PLAYLIST).read[Seq[Track]] and
      (__ \ INDEX).read[Int].map(i => if (i >= 0) Some(i) else None)
    )(StatusEvent.apply _)
}