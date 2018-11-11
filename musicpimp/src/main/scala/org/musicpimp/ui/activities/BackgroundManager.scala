package org.musicpimp.ui.activities

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import com.mle.concurrent.FutureOps
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.TR
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.audio.Track
import org.musicpimp.network.DiscoGs
import org.musicpimp.util.PimpLog

class BackgroundManager(activity: Activity) extends PimpLog {
  private lazy val help = new ActivityHelper(activity)

  def backgroundViewOpt: Option[View] = help.tryFindView(TR.rootPager)

  def setBackground(trackOpt: Option[Track]): Unit =
    for {
      track <- trackOpt
      pager <- backgroundViewOpt
    } {
      DiscoGs.client.cover(track.artist, track.album).map { file =>
        val background = Drawable.createFromPath(file.getAbsolutePath)
        background setAlpha 26
        help.onUiThread(pager.setBackgroundDrawable(background))
      }.recoverAll { t =>
        warn(s"Unable to obtain cover of ${track.artist} - ${track.album}.", t)
      }
    }
}
