package org.musicpimp.network

import java.io.File

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import com.mle.android.util.PreferenceImplicits.RichPrefs
import org.musicpimp.PimpApp
import org.musicpimp.local.LocalLibrary

trait DownloadSettings extends OnSharedPreferenceChangeListener {
  val PREF_DOWNLOADS_DIR = "pref_downloads_dir"
  val prefs = PreferenceManager.getDefaultSharedPreferences(PimpApp.context)
  prefs registerOnSharedPreferenceChangeListener this
  private var dlDir = LocalLibrary.appInternalMusicDir
  var downloadsAbsolutePathPrefix = dlDir.getAbsolutePath + "/"
  var musicBaseDirLength = downloadsAbsolutePathPrefix.length

  def load() = {
    prefs.get(PREF_DOWNLOADS_DIR)
      .map(new File(_))
      .filter(d => d.isDirectory && d.getAbsolutePath != downloadsDir.getAbsolutePath)
      .fold(prefs.edit().putString(PREF_DOWNLOADS_DIR, dlDir.getAbsolutePath).apply())(dir => downloadsDir = dir)
  }

  def downloadsDir_=(newDir: File) = {
    dlDir = newDir
    downloadsAbsolutePathPrefix = dlDir.getAbsolutePath + "/"
    musicBaseDirLength = downloadsAbsolutePathPrefix.length
  }

  def downloadsDir = dlDir

  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String): Unit = {
    if (key == PREF_DOWNLOADS_DIR) {
      load()
    }
  }
}

object DownloadSettings extends DownloadSettings
