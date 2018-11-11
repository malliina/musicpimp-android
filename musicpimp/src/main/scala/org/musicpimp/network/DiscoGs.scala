package org.musicpimp.network

import java.io._

import android.os.Environment
import com.mle.android.http.QuickHttpClient
import com.mle.concurrent.ExecutionContexts.cached
import com.mle.util.WebUtils
import org.musicpimp.util.PimpLog

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
    if (DiscoGs.isStorageMounted) {
      try {
        client.getFile(s"https://api.musicpimp.org/covers?artist=$artistEnc&album=$albumEnc", coverFile(artist, album))
      } catch {
        case e: AssertionError =>
          Future.failed(e)
      }
    } else {
      Future.failed(new Exception(s"Storage not mounted, not downloading cover $artist - $album."))
    }
  }

  protected def coverFile(artist: String, album: String): File = new File(DiscoGs.coverDirectory, s"$artist-$album.jpg")

  def close(): Unit = client.close()
}

object DiscoGs extends PimpLog {
  //  ContextCompat.checkSelfPermission()
  val picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
  val coverDirectory = new File(picDir, "covers")
  val wasCreated = coverDirectory.mkdirs()
  if (!wasCreated) {
    info(s"Created cover dir '$coverDirectory'.")
  } else {
    val state = Environment.getExternalStorageState()
    warn(s"Covers directory not created. Storage state is $state, mounted $isStorageMounted, picture directory '$picDir'.")
  }
  val client = new DiscoGs

  def isStorageMounted = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}
