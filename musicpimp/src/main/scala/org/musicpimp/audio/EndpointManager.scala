package org.musicpimp.audio

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import com.malliina.android.util.PreferenceImplicits.RichPrefs
import java.io.Closeable
import org.musicpimp.PimpApp
import org.musicpimp.http.Endpoint
import org.musicpimp.ui.activities.SettingsBase
import org.musicpimp.util.Keys
import rx.lang.scala.{Subject, Observable}

/** The name of the active endpoint is stored as a preference key.
  */
trait EndpointManager[T <: Closeable] extends OnSharedPreferenceChangeListener {
  protected val eventsSubject = Subject[Changed]()
  val events: Observable[Changed] = eventsSubject
  val prefs = PreferenceManager.getDefaultSharedPreferences(PimpApp.context)
  prefs registerOnSharedPreferenceChangeListener this

  def prefKey: String

  def active: T

  def active_=(newActive: T)

  def default: T

  def buildEndpoint(e: Endpoint): T

  /** Reloads the active endpoint if
    *
    * a) the active endpoint itself is swapped for some other endpoint or
    * b) the set of endpoints is modified (regardless of which endpoint is modified).
    *
    * We only detect changes to endpoints through the event that fires when all endpoints
    * are saved, which could mean that the active endpoint has been modified, so in
    * order not to use outdated endpoint data we reload the endpoint whenever something
    * changes in the configuration.
    *
    */
  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String): Unit = {
    if (key == prefKey || key == Keys.PREF_ENDPOINTS) {
      val old = active
      active = loadActive(sharedPreferences)
      fireEvent(Changed)
      old.close()
    }
  }

  def loadActive(sharedPrefs: SharedPreferences): T =
    sharedPrefs.get(prefKey)
      .flatMap(name => SettingsBase.endpoints(sharedPrefs).find(_.name == name).map(buildEndpoint))
      .getOrElse(default)

  def loadActiveEndpoint(sharedPrefs: SharedPreferences): Option[Endpoint] =
    sharedPrefs.get(prefKey).flatMap(name => SettingsBase.endpoints(sharedPrefs).find(_.name == name))

  def activeEndpoint(sharedPrefs: SharedPreferences) =
    loadActiveEndpoint(sharedPrefs) getOrElse SettingsBase.localEndpoint

  def activate(newActiveEndpointName: String): Unit = prefs.put(prefKey, newActiveEndpointName)

  def fireEvent(event: Changed): Unit = eventsSubject onNext event
}

trait Changed

case object Changed extends Changed
