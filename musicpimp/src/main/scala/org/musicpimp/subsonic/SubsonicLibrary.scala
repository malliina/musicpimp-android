package org.musicpimp.subsonic

import android.net.Uri
import com.mle.android.http.HttpResponse
import com.mle.util.Version
import org.musicpimp.audio.SubsonicHttpClient._
import org.musicpimp.audio._
import org.musicpimp.http.Endpoint
import scala.concurrent.Future

/**
 *
 * @author mle
 */
class SubsonicLibrary(endpoint: Endpoint)
  extends RemoteMediaLibrary(endpoint)
  with SubsonicHttpClient {

  private val json = new SubsonicJsonReaders(endpoint)

  override val reader = json.musicDirReader

  def ping = get[Version](buildPath("ping"))(SubsonicJsonReaders.subsonicVersionReader)

  def folder(id: String): Future[Directory] =
    getWithCache(id)(json.musicDirReader)

  override def rootFolder: Future[Directory] =
    getWithCache(rootFolderId)(json.indexReader)

  def resource(folderId: String): String =
    if (folderId == rootFolderId) buildPath("getIndexes")
    else buildPath("getMusicDirectory", folderId)

  def upload(request: TrackUploadRequest): Future[HttpResponse] = {
    // potential TODO: 1) check if local, if not, download and await completion of download 2) upload from mobile device
    throw new UnsupportedOperationException("Cannot stream tracks when Subsonic is the music source. Set the music source to a MusicPimp server or the local device.")
  }

  override def downloadUri(track: Track): Uri = json.downloadUri(track.id)

  /**
   * This is a helper method for `upload`, but since Subsonic cannot upload anyway, we return a negative result so that
   * it never takes part in it.
   */
  override def cacheContains(trackId: String): Boolean = false
}
