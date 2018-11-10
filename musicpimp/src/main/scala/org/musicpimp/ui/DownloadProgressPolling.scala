package org.musicpimp.ui

import com.mle.android.ui.DownloadHelper
import org.musicpimp.network.DownloadSettings

/** Provides a method `downloadIfNotExists` that initiates downloads and a method
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
  */
trait DownloadProgressPolling extends DownloadHelper {
  def downloadsAbsolutePathPrefix: String = DownloadSettings.downloadsAbsolutePathPrefix

  def musicBaseDirLength: Int = DownloadSettings.musicBaseDirLength
}