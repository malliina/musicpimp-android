package org.musicpimp.subsonic

import android.net.Uri
import com.mle.util.Version
import concurrent.duration._
import org.musicpimp.audio._
import org.musicpimp.http.Endpoint
import org.musicpimp.json.JsonReaders
import org.musicpimp.json.JsonStrings._
import org.musicpimp.json.Readers._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 *
 * @author mle
 */
class SubsonicJsonReaders(endpoint: Endpoint) extends JsonReaders(endpoint) {

  import SubsonicJsonReaders._

  // media may be transcoded
  def uri(trackId: String): Uri = uri2("stream", trackId)

  // original media, never transcoded
  def downloadUri(trackId: String) = uri2("download", trackId)

  private def uri2(methodName: String, trackId: String) =
    Uri.parse(endpoint.httpBaseUri + SubsonicHttpClient.buildPath(methodName, trackId))

  /**
   * The JSON entries may or may not exist, and the values can be strings,
   * integers, whatever, for the same key.

   * Example: "album" for an Iron Maiden track is "Powerslave", but for
   * an Adele track the integer 21. (The name of the album is "21".) The
   * "album" entry might also be missing completely from yet another track.
   *
   * To deal with the issues, the reader reads the entries with `readNullable`,
   * using a reader `stringifier` that accepts both strings and integers.
   *
   * @return
   */
  implicit val subsonicTrackReader: Reads[Track] = (
    (JsPath \ ID).read[Int].map(_.toString) and
      (JsPath \ TITLE).readNullable[String](stringifier).map(_ getOrElse "") and
      (JsPath \ ALBUM).readNullable[String](stringifier).map(_ getOrElse "") and
      (JsPath \ ARTIST).readNullable[String](stringifier).map(_ getOrElse "") and
      (JsPath \ PATH).readNullable[String].map(_ getOrElse "") and
      (JsPath \ DURATION).read[Duration] and
      (JsPath \ SIZE).read[Long] and
      (JsPath \ ID).read[Int].map(i => uri(i.toString)) and
      constant(username) and
      constant(password)
    )(Track)

  val indexReader = new Reads[Directory] {
    def reads(json: JsValue): JsResult[Directory] = {
      val content = json \ SUBSONIC_RESPONSE \ INDEXES
      val folders = ensureIsArray(content \ INDEX).flatMap(_.as[Seq[Folder]](seqFolderReader))
      val tracks = ensureIsArray(content \ CHILD).map(_.as[Track])
      JsSuccess(Directory(folders, tracks))
    }
  }

  implicit val musicDirReader = new Reads[Directory] {
    def reads(json: JsValue): JsResult[Directory] = {
      val content = json \ SUBSONIC_RESPONSE \ DIRECTORY
      // the 'child' entry may or may not exist, may contain one element (no list), or an array of elements
      val jsonArray = ensureIsArray(content \ CHILD)
      val (foldersJson, tracksJson) = jsonArray.partition(j => (j \ IS_DIR).as[Boolean])
      JsSuccess(Directory(foldersJson.map(_.as[Folder](musicDirFolderReader)), tracksJson.map(_.as[Track])))
    }
  }
  // parses the subsonic response after you make a request with params "action=get"
  implicit val statusReader = new Reads[StatusEvent] {
    def reads(json: JsValue): JsResult[StatusEvent] = {
      val content = json \ SUBSONIC_RESPONSE \ JUKEBOX_PLAYLIST
      val isPlaying = (content \ PLAYING).as[Boolean]
      val volume = ((content \ GAIN).as[Float] * 100).toInt
      val playlist = ensureIsArray(content \ ENTRY).map(_.as[Track])
      val indexValue = (content \ CURRENT_INDEX).as[Int]
      val index = if (indexValue >= 0) Some(indexValue) else None
      JsSuccess(StatusEvent(
        track = index.filter(_ < playlist.size).map(playlist),
        state = if (isPlaying) PlayStates.Playing else PlayStates.Stopped,
        position = (content \ POSITION).as[Int].seconds,
        volume = volume,
        mute = volume == 0,
        playlist = playlist,
        playlistIndex = index
      ))
    }
  }

}

object SubsonicJsonReaders {
  val SUBSONIC_RESPONSE = "subsonic-response"
  val JUKEBOX_PLAYLIST = "jukeboxPlaylist"
  val PLAYING = "playing"
  val ENTRY = "entry"
  val CURRENT_INDEX = "currentIndex"
  val IS_DIR = "isDir"
  val DIRECTORY = "directory"
  val CHILD = "child"
  val INDEX = "index"
  val INDEXES = "indexes"
  val ID = "id"
  val NAME = "name"
  val ARTIST = "artist"
  val TITLE = "title"
  val ERROR = "error"
  val MESSAGE = "message"
  val VERSION = "version"
  val STATUS = "status"

  implicit val subsonicVersionReader = new Reads[Version] {
    def reads(json: JsValue): JsResult[Version] =
      (json \ SUBSONIC_RESPONSE).validate[Version]
  }

  implicit val indexFolderReader: Reads[Folder] = (
    (JsPath \ ID).read[Int].map(_.toString) and
      (JsPath \ NAME).read[String]
    )(Folder)

  /**
   * Flattens an array (or not) of artists grouped by their initial letter
   * under an "artists" entry.
   */
  implicit val seqFolderReader = new Reads[Seq[Folder]] {
    def reads(json: JsValue): JsResult[Seq[Folder]] = {
      // this is a seq of json which is seq of (seq of json)
      val groupedArtists = json \\ ARTIST
      JsSuccess(groupedArtists.flatMap(letter => ensureIsArray(letter).map(_.as[Folder])))
    }
  }

  val musicDirFolderReader: Reads[Folder] = (
    (JsPath \ ID).read[Int].map(_.toString) and
      (JsPath \ TITLE).read[String]
    )(Folder)

  /**
   * Attempts to parse a string value, but if that fails, reads and integer and
   * stringifies it, or finally if that also fails, returns an empty string.
   */
  val stringifier = new Reads[String] {
    def reads(json: JsValue): JsResult[String] =
      JsSuccess(json.asOpt[String].getOrElse(json.asOpt[Int].map(_.toString).getOrElse("")))
  }

  /**
   * Subsonic JSON "arrays" are not always arrays. Entries of empty arrays are
   * missing completely, one-element "arrays" are not arrays but just that
   * one element. Only multi-element arrays are actually represented as JSON arrays.
   *
   * @param json json that may or may not be a JSON array as described above
   * @return a guaranteed sequence of json values, no bullshit
   */
  def ensureIsArray(json: JsValue): Seq[JsValue] = json match {
    case JsArray(elements) => elements // JsArray extends JsValue
    case _: JsUndefined => Seq.empty[JsValue] // JsUndefined extends JsValue
    case single: JsValue => Seq(single)
    case _ => Seq.empty[JsValue]
  }
}