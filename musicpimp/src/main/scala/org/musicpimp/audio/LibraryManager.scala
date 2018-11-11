package org.musicpimp.audio

import android.content.SharedPreferences
import com.mle.android.exceptions.ExplainedException
import com.mle.android.util.PreferenceImplicits.RichPrefs
import java.io.File
import org.musicpimp.http._
import org.musicpimp.local.MultiFolderLocalLibrary
import org.musicpimp.pimp.PimpLibrary
import org.musicpimp.subsonic.SubsonicLibrary
import org.musicpimp.ui.activities.{LocalFolders, SettingsBase}
import org.musicpimp.util.{PimpSettings, Keys}

trait LibraryManager extends EndpointManager[MediaLibrary] {
  val settingsHelper = new PimpSettings(prefs)
  override val prefKey = Keys.PREF_LIBRARY
  var localLibrary = loadLocalLibrary
  override val default: MediaLibrary = localLibrary
  override var active: MediaLibrary = loadActive(prefs)

  override def buildEndpoint(e: Endpoint): MediaLibrary = e.endpointType match {
    case EndpointTypes.Local => localLibrary
    case EndpointTypes.MusicPimp => new MasterChildLibrary(new PimpLibrary(e), localLibrary)
    case EndpointTypes.Cloud => new MasterChildLibrary(new PimpLibrary(e), localLibrary)
    case EndpointTypes.Subsonic => new SubsonicLibrary(e)
    case EndpointTypes.MusicBeamer => throw new ExplainedException("A MusicBeamer endpoint cannot function as a music library.")
    case other => throw new ExplainedException(s"Unsupported library endpoint: $other")
  }

  override def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String): Unit = {
    //    info(s"Pref changed: $key")
    super.onSharedPreferenceChanged(sharedPreferences, key)
    if (key == LocalFolders.localFoldersPrefKey) {
      val activeChanged = active == localLibrary
      localLibrary = loadLocalLibrary
      if (activeChanged) {
        active = localLibrary
        fireEvent(Changed)
      }
    }
    setSubsonicLibraryIfPlayerIsSubsonic(sharedPreferences, key)
  }

  def loadLocalLibrary: MultiFolderLocalLibrary = {
    val folders = settingsHelper.loadFolders.map(new File(_)).filter(d => d.isDirectory && d.canRead)
    new MultiFolderLocalLibrary(folders)
  }

  /**
   * If the playback device was changed to a Subsonic server, also sets the library to Subsonic
   * because no other combination is allowed.
   *
   * @param sharedPreferences prefs
   * @param key preference key that changed
   */
  def setSubsonicLibraryIfPlayerIsSubsonic(sharedPreferences: SharedPreferences, key: String) {
    if (key == PlayerManager.prefKey) {
      subsonicPlayerEndpointOpt(sharedPreferences).map(endpoint => {
        // changes the library to the same endpoint as the player
        sharedPreferences.put(prefKey, endpoint.name)
      })
    }
  }

  def subsonicPlayerEndpointOpt(prefs: SharedPreferences): Option[Endpoint] = {
    prefs.get(PlayerManager.prefKey).flatMap(name => SettingsBase.endpoints(prefs)
      .find(_.name == name)
      .filter(_.endpointType == EndpointTypes.Subsonic)
    )
  }

}

object LibraryManager extends LibraryManager
