package org.musicpimp.ui

import java.io.File

import android.app.DownloadManager
import android.net.Uri
import com.mle.android.http.{HttpConstants, HttpUtil}
import com.mle.android.ui.DownloadHelper
import com.mle.util.Utils
import org.musicpimp.audio.{LibraryManager, Track}
import org.musicpimp.network.DownloadSettings

/**
 * Provides a method `downloadIfNotExists` that initiates downloads and a method
 * `onDownloadProgressUpdate` that notifies of download progress updates.
 *
 * Download progress tracking is based on polling and must be started and
 * stopped using the provided methods. The intended usage is to mix this
 * trait into an activity and start/stop polling for updates when the activity
 * is resumed/paused respectively.
 *
 * Inspiration:
 *
 * https://github.com/commonsguy/cw-android/blob/master/Internet/Download/src/com/commonsware/android/download/DownloadDemo.java
 *
 * Note:
 * So I found out that there is a Downloads app, so I do not need to create an Activity that lists them.
 * However, we need to update the progress bar under each track being downloaded and displayed
 * in the library or playlist, so following the progress of downloads is still necessary.
 *
 * @author mle
 */
trait DownloadProgressPolling extends DownloadHelper {

  def downloadsAbsolutePathPrefix: String = DownloadSettings.downloadsAbsolutePathPrefix

  def musicBaseDirLength: Int = DownloadSettings.musicBaseDirLength

  def downloadIfNotExists(tracks: Seq[Track]): Seq[Option[Long]] =
    tracks map downloadIfNotExists

  /**
   * Downloads `track` if its source is remote and it does not exist locally.
   *
   * @param track track to download
   * @return A unique ID for the download if it was submitted, None otherwise.
   */
  def downloadIfNotExists(track: Track): Option[Long] = {
    val source = track.source
    val existsLocally = LibraryManager.localLibrary exists track
    //    info(s"Track $track with path: ${track.path} exists locally: $existsLocally")
    if (!existsLocally && source.isAbsolute && (source.getScheme == "http" || source.getScheme == "https")) {
      download(track)
    } else {
      None
    }
  }

  /**
   * Enqueues a request to download `track` to the local device. The download
   * is handled by the download manager app.
   *
   * @param track track to download
   * @return a unique ID for the download
   */
  def download(track: Track): Option[Long] = {
    // creates destination directory
    val destinationFile = new File(DownloadSettings.downloadsDir, track.path)
    Option(destinationFile.getParentFile).map(_.mkdirs())
    val request = new DownloadManager.Request(track.source)
      .setTitle(track.title)
      .setDescription("MusicPimp download")
      .setDestinationUri(Uri fromFile destinationFile)
      .addRequestHeader(HttpConstants.AUTHORIZATION, HttpUtil.authorizationValue(track.username, track.password))
    // only added in API level 11
    //    request.allowScanningByMediaScanner()
    Utils.opt[Long, SecurityException](downloadManager enqueue request)
  }
}