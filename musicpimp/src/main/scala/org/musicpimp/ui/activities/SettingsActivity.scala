package org.musicpimp.ui.activities

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.{Context, SharedPreferences}
import android.graphics.Color
import android.os.Bundle
import android.preference.{EditTextPreference, ListPreference, PreferenceActivity}
import android.view.View
import android.widget.Button
import com.malliina.android.exceptions.ExplainedException
import com.malliina.android.ui.Implicits.action2clickListener
import com.malliina.android.util.PreferenceImplicits.RichPrefs
import org.musicpimp.R
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.audio.LibraryManager
import org.musicpimp.network.DownloadSettings
import org.musicpimp.util.Keys._
import org.musicpimp.util.PimpSettings

class SettingsActivity
  extends PreferenceActivity
  with OnSharedPreferenceChangeListener {

  lazy val helper = new ActivityHelper(this)

  lazy val settingsHelper = new PimpSettings(helper.prefs)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.preferences)

    val endpointsButton = newNavButton(R.string.endpoints_desc, helper.navigate(classOf[Endpoints]))
    val foldersButton = newNavButton(R.string.folders_button_desc, helper.navigate(classOf[LocalFolders]))
    val alarmsButton = newNavButton(R.string.alarms_desc, helper.navigate(classOf[AlarmsActivity]))

    addFooterViews(endpointsButton, foldersButton, alarmsButton)
  }

  private def addFooterViews(views: View*): Unit = {
    val listView = getListView
    views foreach listView.addFooterView
  }

  def newNavButton(textRes: Int, onClick: => Unit): Button = {
    val b = new Button(this)
    b setTextColor Color.WHITE
    b setBackgroundResource R.drawable.selector_transparent_button
    b setText textRes
    b setOnClickListener (() => onClick)
    b
  }

  override def onResume() {
    helper.prefs registerOnSharedPreferenceChangeListener this
    super.onResume()
    updatePrefs()
    updateLibraryEditability()
  }


  override def onPause(): Unit = {
    super.onPause()
    helper.prefs unregisterOnSharedPreferenceChangeListener this
  }

  def updatePrefs() {
    initListAndEnsurePrefIsSet(PREF_LIBRARY, settingsHelper.sources)
    initListAndEnsurePrefIsSet(PREF_PLAYER, settingsHelper.players)
    val cachePref = findPreference(PREF_CACHE).asInstanceOf[EditTextPreference]
    val gbCount = cachePref.getText
    cachePref setSummary cacheSummary(gbCount)
    initList(DownloadSettings.PREF_DOWNLOADS_DIR, settingsHelper.loadFolders.toArray)
  }

  def updateLibraryEditability(): Unit =
    libraryPreference setEnabled canChangeLibrary

  def canChangeLibrary: Boolean =
    LibraryManager.subsonicPlayerEndpointOpt(helper.prefs).isEmpty

  private def sharedPrefs = getPreferenceScreen.getSharedPreferences

  private def libraryPreference = getPreferenceScreen getPreference 0

  private def initListAndEnsurePrefIsSet(key: String, entries: Array[CharSequence]) {
    val (pref, isSet) = initList(key, entries)
    if (!isSet) {
      // We get here if the active entry no longer exists.
      // The first entry in entries is the local device.
      entries.headOption.fold[Unit](throw new ExplainedException(s"No entries found for preference: $key. At least one is required."))(localName => {
        pref setValue localName.toString
        sharedPrefs.put(key, localName.toString)
      })
    }
    update(key)
  }

  private def initList(key: String, entries: Array[CharSequence]) = {
    val pref = findPreference(key).asInstanceOf[ListPreference]
    pref setEntries entries
    pref setEntryValues entries
    (pref, Option(pref.getEntry).map(summary => pref setSummary summary).isDefined)
  }

  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    if (key == PREF_LIBRARY || key == PREF_PLAYER) {
      update(key)
    }
    if (key == PREF_CACHE) {
      val size = sharedPreferences.getString(PREF_CACHE, "5")
      findPreference(PREF_CACHE) setSummary cacheSummary(size)
    }
  }

  def update(key: String) = {
    helper.prefs.get(key).map(prefValue => {
      val pref = findPreference(key).asInstanceOf[ListPreference]
      pref setSummary prefValue
      // if the preference is changed programmatically, its value needs to be updated
      Option(pref.getEntry).filter(_ == prefValue).getOrElse {
        pref setValue prefValue
      }
      if (key == PREF_PLAYER) {
        updateLibraryEditability()
      }
    })
  }

  private def cacheSummary(value: String) = s"$value gigabytes"
}

object SettingsActivity {
  def prefs(ctx: Context) = ctx.getSharedPreferences(PREF_ENDPOINTS, Context.MODE_PRIVATE)
}
