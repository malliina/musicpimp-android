package org.musicpimp.local

import android.net.Uri
import com.mle.android.http.{AuthHttpClient, HttpResponse}
import com.mle.util.Utils.executionContext
import com.mle.util.Version
import concurrent.duration._
import java.io.{FileNotFoundException, File}
import org.musicpimp.PimpApp
import org.musicpimp.audio.{TrackUploadRequest, Track, MediaLibrary}
import org.musicpimp.pimp.PimpLibrary
import scala.concurrent.Future

/**
 *
 * @author mle
 */
trait LocalLibraryBase extends MediaLibrary {
  override val isLocal = true
  val ping: Future[Version] = Future.successful(PimpApp.version)

  def findPath(relativePath: String): Option[File]

  def findPath(track: Track): Option[File] =
    findPath(track.path)

  def path(relativePath: String): File =
    findPath(relativePath)
      .getOrElse(throw new FileNotFoundException(s"Unable to find file or directory at path: $relativePath"))

  def path(track: Track): File = path(track.path)

  def findLocalUri(t: Track) = findPath(t).map(Uri.fromFile)

  def exists(t: Track) = findLocalUri(t).isDefined

  def upload(request: TrackUploadRequest): Future[HttpResponse] = {
    //    info(s"Local lib will upload $request")
    val file = path(PimpLibrary.pathFromId(request.track))
    val httpClient = new AuthHttpClient(request.username, request.password)
    httpClient.httpClient setTimeout (6 minutes).toMillis.toInt
    val ret = httpClient.postFile(request.uri, file)
    ret.onComplete(_ => httpClient.close())
    ret
  }

  def cacheContains(trackId: String): Boolean = findPath(PimpLibrary.pathFromId(trackId)).isDefined
}
