package org.musicpimp.network

import java.io._

import android.os.Environment
import com.mle.android.http.QuickHttpClient
import com.mle.concurrent.ExecutionContexts.cached
import com.mle.util.WebUtils

import scala.concurrent.Future

class DiscoGs extends Closeable {
  val iLoveDiscoGsFakeCoverSize = 15378
  val client = new QuickHttpClient

  /** Returns the album cover. Optionally downloads it if it doesn't already exist locally.
    *
    * @return the album cover file, which is an image
    */
  def cover(artist: String, album: String): Future[File] = {
    val file = coverFile(artist, album)
    if (file.canRead && file.length() != iLoveDiscoGsFakeCoverSize) Future.successful(file)
    else downloadCover(artist, album).filter(_.length() != iLoveDiscoGsFakeCoverSize)
  }

  def downloadCover(artist: String, album: String): Future[File] = {
    val artistEnc = WebUtils.encodeURIComponent(artist)
    val albumEnc = WebUtils.encodeURIComponent(album)
    client.getFile(s"https://api.musicpimp.org/covers?artist=$artistEnc&album=$albumEnc", coverFile(artist, album))
  }

  protected def coverFile(artist: String, album: String): File = new File(DiscoGs.coverDirectory, s"$artist-$album.jpg")

  def close(): Unit = client.close()
}

object DiscoGs {
  val coverDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "covers")
  coverDirectory.mkdirs()
  val client = new DiscoGs
}
