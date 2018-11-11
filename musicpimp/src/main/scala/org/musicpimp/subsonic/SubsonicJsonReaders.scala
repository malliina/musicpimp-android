package org.musicpimp.subsonic

import android.net.Uri
import com.mle.util.Version
import org.musicpimp.audio._
import org.musicpimp.http.Endpoint
import org.musicpimp.json.JsonReaders
import org.musicpimp.json.JsonStrings._
import org.musicpimp.json.Readers._
import org.musicpimp.util.PimpLog
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.duration._

class SubsonicJsonReaders(endpoint: Endpoint) extends JsonReaders(endpoint) with PimpLog {

  import org.musicpimp.subsonic.SubsonicJsonReaders._

  // media may be transcoded
  def uri(trackId: String): Uri = uri2("stream", trackId)

  // original media, never transcoded
  def downloadUri(trackId: String) = uri2("download", trackId)

  private def uri2(methodName: String, trackId: String) =
    Uri.parse(endpoint.httpBaseUri + SubsonicHttpClient.buildPath(methodName, trackId))


  /** The JSON entries may or may not exist, and the values can be strings,
    * integers, whatever, for the same key.
    *
    * Example: "album" for an Iron Maiden track is "Powerslave", but for
    * an Adele track the integer 21. (The name of the album is "21".) The
    * "album" entry might also be missing completely from yet another track.
    *
    * To deal with the issues, the reader reads the entries with `readNullable`,
    * using a reader `stringifier` that accepts both strings and integers.
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
      constantT(username) and
      constantT(password) and
      constantT(cloudID)
    ) (Track)

  val indexReader = new Reads[Directory] {
    def reads(json: JsValue): JsResult[Directory] = {
      val content = json \ SUBSONIC_RESPONSE \ INDEXES
      for {
        folders <- ensureValidateArray[Seq[Folder]](content \ INDEX)(artistGroupedFolderReader).map(_.flatten)
        tracks <- ensureValidateArray[Track](content \ CHILD)
      } yield Directory(folders, tracks)
    }
  }

  implicit val musicDirReader = new Reads[Directory] {
    def reads(json: JsValue): JsResult[Directory] = {
      val content = json \ SUBSONIC_RESPONSE \ DIRECTORY
      // the 'child' entry may or may not exist, may contain one element (no list), or an array of elements
      val jsonArray = ensureIsArray(content \ CHILD)
      val (foldersJson, tracksJson) = jsonArray.partition(j => (j \ IS_DIR).validate[Boolean].getOrElse(false)) // ???
      for {
        folders <- validateArray2[Folder](foldersJson)(musicDirFolderReader)
        tracks <- validateArray2[Track](tracksJson)
      } yield Directory(folders, tracks)
    }
  }
  val searchResultReader = new Reads[Seq[Track]] {
    override def reads(json: JsValue): JsResult[Seq[Track]] = {
      ensureValidateArray[Track](json \ SUBSONIC_RESPONSE \ SEARCH_RESULT2 \ SONG)
    }
  }
  // parses the subsonic response after you make a request with params "action=get"
  implicit val statusReader = new Reads[StatusEvent] {
    def reads(json: JsValue): JsResult[StatusEvent] = {
      val content = json \ SUBSONIC_RESPONSE \ JUKEBOX_PLAYLIST
      for {
        isPlaying <- (content \ PLAYING).validate[Boolean]
        volume <- (content \ GAIN).validate[Float].map(_ * 100).map(_.toInt)
        playlist <- JsArray(ensureIsArray(content \ ENTRY)).validate[Seq[Track]]
        indexValue <- (content \ CURRENT_INDEX).validate[Int]
        position <- (content \ POSITION).validate[Int].map(_.seconds)
      } yield {
        val index = if (indexValue >= 0) Some(indexValue) else None
        StatusEvent(
          track = index.filter(_ < playlist.size).map(playlist),
          state = if (isPlaying) PlayStates.Playing else PlayStates.Stopped,
          position = position,
          volume = volume,
          mute = volume == 0,
          playlist = playlist,
          playlistIndex = index
        )
      }
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
  val FAILED = "failed"
  val SEARCH_RESULT2 = "searchResult2"
  val SONG = "song"

  implicit val subsonicVersionReader = new Reads[Version] {
    def reads(json: JsValue): JsResult[Version] =
      (json \ SUBSONIC_RESPONSE).validate[Version]
  }

  implicit val indexFolderReader: Reads[Folder] = (
    (JsPath \ ID).read[Int].map(_.toString) and
      (JsPath \ NAME).read[String]
    ) (Folder)

  /** Flattens an array (or not) of artists grouped by their initial letter under an "artists" entry.
    */
  implicit val artistGroupedFolderReader = new Reads[Seq[Folder]] {
    def reads(json: JsValue): JsResult[Seq[Folder]] = {
      // this is a seq of json which is seq of (seq of json)
      val groupedArtists = json \\ ARTIST
      flatten(groupedArtists.map(letter => ensureValidateArray[Folder](letter)).toList).map(_.flatten)
    }
  }

  val musicDirFolderReader: Reads[Folder] = (
    (JsPath \ ID).read[Int].map(_.toString) and
      (JsPath \ TITLE).read[String]
    ) (Folder)

  /** Attempts to parse a string value, but if that fails, reads and integer and
    * stringifies it, or finally if that also fails, returns an empty string.
    */
  val stringifier = new Reads[String] {
    def reads(json: JsValue): JsResult[String] =
      JsSuccess(json.asOpt[String].getOrElse(json.asOpt[Int].map(_.toString).getOrElse("")))
  }

  /** Subsonic JSON "arrays" are not always arrays. Entries of empty arrays are
    * missing completely, one-element "arrays" are not arrays but just that
    * one element. Only multi-element arrays are actually represented as JSON arrays.
    *
    * @param json json that may or may not be a JSON array as described above
    * @return a guaranteed sequence of json values, no bullshit
    */
  def ensureIsArray(json: JsValue): Seq[JsValue] = json match {
    case JsString(v) if v.isEmpty => Seq.empty[JsValue]
    case JsArray(elements) => elements // JsArray extends JsValue
    case _: JsUndefined => Seq.empty[JsValue] // JsUndefined extends JsValue
    case single: JsValue => Seq(single)
    case _ => Seq.empty[JsValue]
  }

  def toArray(json: JsValue) = JsArray(ensureIsArray(json))

  def ensureValidateArray[T](json: JsValue)(implicit reader: Reads[T]): JsResult[Seq[T]] =
    validateArray2[T](ensureIsArray(json))

  def validateArray2[T](json: Seq[JsValue])(implicit reader: Reads[T]): JsResult[Seq[T]] =
    JsArray(json).validate[Seq[T]]

  def flatten[T](results: List[JsResult[T]]): JsResult[List[T]] = results match {
    case Nil => JsSuccess(Nil)
    case head :: tail => head.flatMap(t => flatten(tail).map(r => t :: r))
  }
}