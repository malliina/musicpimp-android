package org.musicpimp.ui.activities

import android.app.{Activity, DownloadManager}
import android.content.IntentFilter
import org.musicpimp.ui.MusicDownloadUpdating

trait MusicDownloadUpdatingActivity extends Activity with MusicDownloadUpdating {

  override def activity: Activity = this

  override def onResume(): Unit = {
    super.onResume()
    registerReceiver(downloadCompleteListener, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    startPollDownloadProgress()
  }

  override def onPause(): Unit = {
    unregisterReceiver(downloadCompleteListener)
    stopPollDownloadProgress()
    super.onPause()
  }
}
