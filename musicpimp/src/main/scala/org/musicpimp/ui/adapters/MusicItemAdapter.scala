package org.musicpimp.ui.adapters

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import com.mle.andro.ui.adapters.IconOneLineAdapter
import com.mle.android.exceptions.ExplainedException
import org.musicpimp.audio.{Folder, Track, MusicItem}
import org.musicpimp.util.PimpLog
import org.musicpimp.{TR, R}

/**
 *
 * @author mle
 */
class MusicItemAdapter(ctx: Context, folders: Seq[MusicItem], tracks: Seq[TrackItem])
  extends IconOneLineAdapter[MusicItem](ctx, R.layout.music_item, folders ++ tracks)
  with PimpLog {

  override def decorate(view: View, item: MusicItem, position: Int): Unit = {
    super.decorate(view, item, position)
    item match {
      case t: TrackItem => updateProgress(view, t)
      case _ => ()
    }
  }

  private def updateProgress(view: View, item: TrackItem) {
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

  override def imageResource(item: MusicItem, pos: Int) = item match {
    case _: TrackItem => android.R.drawable.ic_media_play
    case _: Folder => R.drawable.holofolder
    case _ => throw new ExplainedException(s"Unknown music item: $item")
  }

  override def firstRow(item: MusicItem, pos: Int) = item.title

  def findTrack(path: String) = tracks.find(_.track.path == path)
}

/**
 * Wraps a track with download progress info so we can display a progress bar under each track as it's being downloaded.
 *
 * @param track the track
 * @param progress download progress of the track
 */
case class TrackItem(track: Track, var progress: DownloadProgress) extends MusicItem {
  val id = track.id
  val title = track.title
}

case class DownloadProgress(bytes: Long, total: Long, transferring: Boolean)

object DownloadProgress {
  val empty = new DownloadProgress(0, 0, transferring = false)
}