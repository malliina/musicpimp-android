package org.musicpimp.usage

import android.preference.PreferenceManager
import com.mle.android.util.PreferenceImplicits.RichPrefs
import concurrent.duration._
import org.musicpimp.PimpApp
import org.musicpimp.util.Keys

trait PimpUsageController extends UsageController {
  // obviously, this must always be true for Google Play / Amazon / Samsung builds
  private val isIapEnabled = true

  val upFrontAllowance = 10
  val timeWindow = 24.hours
  val timeWindowLimit = 3

  var allowUnlimited = false

  private val locals = new UsageList(Keys.PREF_LOCAL_PLAYBACK, timeWindowLimit, timeWindow)

  private val remotes = new UsageList(Keys.PREF_REMOTE_PLAYBACK, timeWindowLimit, timeWindow)

  def isLocalPlaybackAllowed: Boolean = allowPlayback(locals.allow)

  def isRemotePlaybackAllowed: Boolean = allowPlayback(remotes.allow)

  def localPlaybackStarted(): Unit = {
    locals.prependCurrentTime()
    incrementPlaybackCount()
  }

  def remotePlaybackStarted(): Unit = {
    remotes.prependCurrentTime()
    incrementPlaybackCount()
  }

  def totalPlaybackCount =
    prefs.getInt(Keys.PREF_PLAYBACK_COUNT, 0)

  def incrementPlaybackCount(): Int = {
    val newCount = totalPlaybackCount + 1
    prefs.putInt(Keys.PREF_PLAYBACK_COUNT, newCount)
    newCount
  }

  private def prefs = PreferenceManager.getDefaultSharedPreferences(PimpApp.context)

  private def allowPlayback(p: => Boolean) = {
    //    info(s"IAP enabled: $isIapEnabled, allow unlimited: $allowUnlimited, count: $totalPlaybackCount")
    !isIapEnabled || allowUnlimited || totalPlaybackCount <= upFrontAllowance || p
  }
}

object PimpUsageController extends PimpUsageController
