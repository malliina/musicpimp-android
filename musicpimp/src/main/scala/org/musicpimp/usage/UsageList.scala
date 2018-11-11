package org.musicpimp.usage

import com.mle.android.util.PersistentList
import org.musicpimp.PimpApp
import scala.concurrent.duration.Duration

class UsageList(key: String, limit: Int, timeWindow: Duration) extends PersistentList[Long](PimpApp.context, key) {
  private val timeWindowMillis = timeWindow.toMillis

  def prependCurrentTime() = prepend(System.currentTimeMillis())

  def allow: Boolean = get.size < limit || !isWithinTimeWindow(get take limit)

  private def isWithinTimeWindow(times: Seq[Long]) =
    times.forall(time => System.currentTimeMillis() - time < timeWindowMillis)
}