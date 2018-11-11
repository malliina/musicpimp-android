package org.musicpimp.ui.fragments

import android.app.DownloadManager
import android.content.IntentFilter
import android.support.v4.app.Fragment
import org.musicpimp.ui.MusicDownloadUpdating

/** Download progress updater that starts polling on `onResume` and stops
  * polling on `onPause`.
  */
trait MusicDownloadUpdatingFragment extends Fragment with MusicDownloadUpdating {
  override def onResume() {
    super.onResume()
    activity.registerReceiver(downloadCompleteListener, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    startPollDownloadProgress()
  }

  override def onPause() {
    activity.unregisterReceiver(downloadCompleteListener)
    stopPollDownloadProgress()
    super.onPause()
  }
}
