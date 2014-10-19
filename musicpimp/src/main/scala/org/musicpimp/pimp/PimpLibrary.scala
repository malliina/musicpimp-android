package org.musicpimp.pimp

import java.io.File
import java.net.{URLDecoder, URLEncoder}

import android.net.Uri
import com.mle.android.http.HttpResponse
import com.mle.util.Utils.executionContext
import com.mle.util.Version
import org.musicpimp.PimpApp
import org.musicpimp.audio._
import org.musicpimp.http.Endpoint
import play.api.libs.json.{Json, Reads}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 *
 * @author mle
 */
class PimpLibrary(endpoint: Endpoint) extends RemoteMediaLibrary(endpoint) with PimpHttpClient {

  import org.musicpimp.pimp.PimpLibrary._

  implicit val reader: Reads[Directory] = json.dirReader

  /**
   * An HTTP client with a 6 minute timeout, all other settings are the same as for the normal enclosing client.
   *
   * Needed for uploads that take a long time, like when streaming to a remote player.
   */
  val longTimeoutClient = new PimpWebHttpClient(endpoint)
  longTimeoutClient.httpClient setTimeout (6 minutes).toMillis.toInt

  def pingNoAuth: Future[Unit] = client getEmpty pingResource

  def ping: Future[Version] = get[Version](pingAuthResource)

  def folder(id: String): Future[Directory] = getWithCache(id)

  def search(term: String, limit: Int = defaultSearchLimit): Future[Seq[Track]] = {
    implicit val trackReader = json.pimpTrackReader
    get[Seq[Track]](s"/search?term=$term&limit=$limit")
  }

  def uri(trackId: String): Uri = json uri trackId

  def resource(folderId: String): String =
    if (folderId == rootFolderId) baseResource
    else s"$baseResource/$folderId"

  def upload(request: TrackUploadRequest): Future[HttpResponse] = {
    //    info(s"Instructing $endpoint to upload $request...")
    longTimeoutClient.post(PimpApp.context, beamStreamResource, Json.toJson(request)).map(response => {
      //      info(s"Instructed $endpoint to upload track ${request.track} to MusicBeamer.")
      response
    })
  }

  /**
   * Checks if track `trackId` exists in the cache.
   *
   * @param trackId ID of track
   * @return true if the cache contains a track with ID `trackId`, false otherwise
   */
  override def cacheContains(trackId: String): Boolean = {
    // calculates the directory ID from the track ID
    val path = pathFromId(trackId)
    val dirId = encode(Option(new File(path).getParent) getOrElse "")
    // checks whether directory `dirId` exists and if found, checks whether it contains track `trackId`
    cache.get(dirId).exists(_.tracks.exists(_.id == trackId))
  }
}

object PimpLibrary {
  val (baseResource, pingResource, pingAuthResource, beamStreamResource) =
    ("/folders", "/ping", "/pingauth", "/playback/stream")

  val UTF8 = "UTF-8"

  def encode(id: String) = if (id.isEmpty) id else URLEncoder.encode(id.replace('/', '\\'), UTF8)

  def pathFromId(path: String) = URLDecoder.decode(path, UTF8).replace('\\', '/')
}

