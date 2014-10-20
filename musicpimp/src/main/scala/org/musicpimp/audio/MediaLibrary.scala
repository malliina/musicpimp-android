package org.musicpimp.audio

import java.io.Closeable

import com.mle.android.http.HttpResponse
import com.mle.util.Utils.executionContext
import com.mle.util.Version

import scala.concurrent.Future

/**
 *
 * @author mle
 */
trait MediaLibrary extends Closeable {
  val defaultSearchLimit = 100

  def isLocal: Boolean = false

  def invalidateCache(): Unit = ()

  /**
   * Pings the server.
   *
   * @return the server version
   */
  def ping: Future[Version]

  /**
   * Caches the metadata of the entire music library if prefetching is supported.
   *
   * In any case the root folder should be returned, so if caching is supported,
   * the cache must be updated as a side effect.
   *
   * @return the contents of the root folder
   */
  def prefetch(): Future[Directory] = rootFolder

  /**
   * @return the contents of the root folder
   */
  def rootFolder: Future[Directory]

  /**
   *
   * @param id the folder id
   * @return the contents of the folder with the given id, or an empty folder if no such folder exists
   */
  def folder(id: String): Future[Directory]

  /**
   * If the library does not contain a folder with the given id, the behavior is undefined.
   *
   * @param folder the folder id
   * @return all the tracks in the folder, recursively
   */
  def tracksIn(folder: Folder): Future[Seq[Track]] =
    tracksIn(folder.id)

  def tracksIn(folderId: String): Future[Seq[Track]] = {
    val dir = folder(folderId)
    val shallowTracks = dir.map(_.tracks)
    // gets all tracks from subdirectories, recursively, in parallel
    // flatMap flattens a Future-of-Future, flatten flattens a Seq-of-Seq
    val subTracks = dir.flatMap(dir => Future.traverse(dir.folders)(tracksIn).map(_.flatten))
    Future.sequence(Seq(subTracks, shallowTracks)).map(_.flatten)
  }

  def search(term: String, limit: Int): Future[Seq[Track]] = Future.successful(Nil)

  def search(term: String): Future[Seq[Track]] = search(term, defaultSearchLimit)

  /**
   * The URI to the original media of `track`. Defaults to track.source. However, track.source is the playback URI,
   * which may return transcoded data, not the original media. Therefore, override this to return the original media if
   * transcoding is possibly enabled, as may be the case with Subsonic.
   *
   * @param track track
   * @return the URI to the original media
   */
  def downloadUri(track: Track) = track.source

  /**
   * Instructs this library to perform a multipart/form-data upload of a track to a given destination.
   *
   * Details about the track and the upload destination are given in `request`.
   *
   * @param request details of the upload, containing the source track, destination uri and destination credentials
   */
  def upload(request: TrackUploadRequest): Future[HttpResponse]

  def cacheContains(trackId: String): Boolean

  /**
   * Closes any resources. This library must not be used after
   * this method is called. Never throws.
   */
  def close(): Unit = ()
}

