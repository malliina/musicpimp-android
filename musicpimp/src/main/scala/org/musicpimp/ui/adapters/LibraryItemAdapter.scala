package org.musicpimp.ui.adapters

import android.content.Context
import android.view.View
import com.mle.andro.ui.adapters.IconOneLineAdapter
import com.mle.android.exceptions.ExplainedException
import org.musicpimp.R
import org.musicpimp.audio.{Folder, MusicItem}
import org.musicpimp.util.PimpLog

class LibraryItemAdapter(ctx: Context, folders: Seq[MusicItem], val tracks: Seq[TrackItem])
  extends IconOneLineAdapter[MusicItem](ctx, R.layout.music_item, folders ++ tracks)
  with MusicItemAdapterBase[MusicItem]
  with PimpLog {

  override def decorate(view: View, item: MusicItem, position: Int): Unit = {
    super.decorate(view, item, position)
    item match {
      case t: TrackItem => updateProgress(view, t)
      case _ => ()
    }
  }

  override def imageResource(item: MusicItem, pos: Int) = item match {
    case _: TrackItem => android.R.drawable.ic_media_play
    case _: Folder => R.drawable.holofolder
    case _ => throw new ExplainedException(s"Unknown music item: $item")
  }

  override def firstRow(item: MusicItem, pos: Int) = item.title
}