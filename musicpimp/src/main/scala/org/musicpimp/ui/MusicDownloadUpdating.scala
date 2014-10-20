package org.musicpimp.ui

import android.app.{Activity, DownloadManager}
import android.content.IntentFilter
import android.support.v4.app.Fragment
import com.mle.andro.ui.adapters.BaseArrayAdapter
import com.mle.android.receivers.DownloadCompleteListener
import com.mle.android.ui.DownloadStatus
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.ui.adapters.{MusicItemAdapterBase, LibraryItemAdapter}

/**
 * This trait updates the download progress of music items loaded into an adapter.
 *
 */
trait MusicDownloadUpdating extends DownloadProgressPolling {
  def activity: Activity

  private lazy val help = new ActivityHelper(activity)

  val downloadCompleteListener = new DownloadCompleteListener {
    def onDownloadComplete(id: Long): Unit = MusicDownloadUpdating.this.onDownloadComplete(id)
  }

  /**
   * Analyzes the updated download statuses and updates the progress of any
   * items loaded to the adapter so that the progress can be shown in the UI.
   *
   * Impl: Note how the first generator in a for comprehension is translated to a flatMap,
   * however we want Seq.flatMap, not Option.flatMap because the following
   * map will be on a sequence for the second generator. Thus we use the trick of
   * calling `toSeq` on the option, otherwise this would not compile. More info:
   *
   * http://stackoverflow.com/questions/4719592/type-mismatch-on-scala-for-comprehension
   *
   * @param downloads all downloads with status STATUS_RUNNING
   */
  override def onDownloadProgressUpdate(downloads: Seq[DownloadStatus]): Unit =
    for {
      adapter <- adapterOpt[MusicItemAdapterBase[_]].toSeq
      download <- downloads
      trackItem <- adapter.findTrack(download.localPath)
    } {
      trackItem.progress = trackItem.progress.copy(bytes = download.bytesDownloaded, total = download.totalSizeBytes, transferring = true)
      help.onUiThread(adapter.notifyDataSetChanged())
    }

  def onDownloadComplete(id: Long): Unit = {
    for {
      download <- queryStatus(id)
      adapter <- adapterOpt[MusicItemAdapterBase[_]]
      trackItem <- adapter.findTrack(download.localPath)
    } {
      trackItem.progress = trackItem.progress.copy(transferring = false)
      help.onUiThread(adapter.notifyDataSetChanged())
    }
  }

  def adapterOpt[T]: Option[T]
}


