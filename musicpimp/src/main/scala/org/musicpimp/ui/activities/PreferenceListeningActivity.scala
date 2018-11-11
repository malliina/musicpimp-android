package org.musicpimp.ui.activities

import android.content.SharedPreferences.OnSharedPreferenceChangeListener

trait PreferenceListeningActivity extends LayoutBaseActivity with OnSharedPreferenceChangeListener {
  override def onResume(): Unit = {
    prefs registerOnSharedPreferenceChangeListener this
    super.onResume()
  }

  override def onPause(): Unit = {
    super.onPause()
    prefs unregisterOnSharedPreferenceChangeListener this
  }
}
