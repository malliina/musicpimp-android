package org.musicpimp.ui.adapters

import android.content.Context
import com.mle.andro.ui.adapters.IconTwoLinesAdapter
import org.musicpimp.pimp.Alarms.Alarm

/**
 * @author Michael
 */
class AlarmsAdapter(ctx: Context, alarms: Seq[Alarm]) extends IconTwoLinesAdapter[Alarm](ctx, alarms) {
  override def firstRow(item: Alarm, position: Int): String = item.job.track.title

  private def onOrOff(item: Alarm) = if (item.enabled) "ON: " else "OFF: "

  override def secondRow(item: Alarm, pos: Int): String = onOrOff(item) + item.when.describe

  override def imageResource(item: Alarm, position: Int): Int = android.R.drawable.ic_menu_edit
}