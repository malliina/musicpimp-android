package org.musicpimp.ui.adapters

import android.view.View
import android.widget.ProgressBar
import com.mle.andro.ui.adapters.BaseArrayAdapter
import org.musicpimp.TR
import org.musicpimp.audio.MusicItem

trait MusicItemAdapterBase[T <: MusicItem] extends BaseArrayAdapter[T] {
  def tracks: Seq[TrackItem]

  def findTrack(path: String) = tracks.find(_.track.path == path)

  protected def updateProgress(view: View, item: TrackItem) {
    val progress = item.progress
    val transferring = progress.transferring
    def progressVisibility = if (transferring) View.VISIBLE else View.INVISIBLE
    val progressBar = findView(view, TR.progressBar.id).asInstanceOf[ProgressBar]
    progressBar setVisibility progressVisibility
    if (transferring) {
      val bytes = progress.bytes.toInt
      if (bytes != progressBar.getProgress) {
        val total = progress.total
        val shownTotal = if (total == -1) item.track.size else total
        progressBar setMax shownTotal.toInt
        progressBar setProgress bytes
        //        debug(s"Updated progress of ${item.title} to $bytes / $total")
      }
    }
  }
}
