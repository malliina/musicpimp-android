package org.musicpimp.ui.adapters

import android.content.Context
import android.view.View
import com.mle.andro.TR
import com.mle.andro.ui.adapters.IconOneLineAdapter
import org.musicpimp.R

/**
 * @author Michael
 */
class SearchAdapter(ctx: Context, val tracks: Seq[TrackItem])
  extends IconOneLineAdapter[TrackItem](ctx, R.layout.search_result_track, tracks)
  with MusicItemAdapterBase[TrackItem] {
  override def imageResource(item: TrackItem, position: Int): Int = android.R.drawable.ic_media_play

  override def firstRow(item: TrackItem, position: Int): String = item.title

  def secondRow(item: TrackItem, pos: Int): String = item.artist

  override def decorate(view: View, item: TrackItem, position: Int): Unit = {
    super.decorate(view, item, position)
    findTypedView(view, TR.secondLine) setText secondRow(item, position)
    updateProgress(view, item)
  }
}
