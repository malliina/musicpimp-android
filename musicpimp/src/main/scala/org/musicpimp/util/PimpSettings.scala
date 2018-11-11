package org.musicpimp.util

import android.content.SharedPreferences
import com.malliina.android.util.PreferenceImplicits.RichPrefs
import org.musicpimp.andro.util.SettingsHelper
import org.musicpimp.audio.LibraryManager
import org.musicpimp.http.Endpoint
import org.musicpimp.local.MultiFolderLocalLibrary
import org.musicpimp.ui.activities.{SettingsBase, LocalFolders}

class PimpSettings(prefs: SharedPreferences) extends SettingsHelper(prefs) {
  def loadFolders = loadSeq[String](LocalFolders.localFoldersPrefKey)
    .getOrElse(MultiFolderLocalLibrary.defaultFolderPaths)

  def saveEndpoints(endpoints: Seq[Endpoint]) = SettingsBase.saveEndpoints(prefs, endpoints)

  def saveEndpoint(endpoint: Endpoint, saveFunc: Endpoint => Option[String], activate: => Boolean): Option[String] = {
    saveFunc(endpoint).fold({
      if (activate) {
        LibraryManager.activate(endpoint.name)
      }
      Option.empty[String]
    })(msg => Some(msg))
  }

  def userAddedEndpoints: Seq[Endpoint] = SettingsBase.userAddedEndpoints(prefs)

  def endpoints: Seq[Endpoint] =
    SettingsBase.endpoints(prefs)

  def sources: Array[CharSequence] =
    SettingsBase.localEndpointNames ++ userAddedEntries(_.supportsSource)

  def players: Array[CharSequence] =
    SettingsBase.inbuiltEndpointNames ++ userAddedEntries(_ => true)

  def userAddedEntries(p: Endpoint => Boolean): Array[CharSequence] =
    userAddedEndpoints.filter(p).map(_.name).toArray

  def addEndpoint(endpoint: Endpoint): Option[String] = {
    val existing = userAddedEndpoints
    save(existing, endpoint)
  }

  /**
   * Updates configuration `old` with `updated`. If the endpoint is an active endpoint, any updates will occur
   * automatically and asynchronously.
   *
   * @param old old settings
   * @param updated new settings
   * @return any error message, or [[None]] if the operation succeeded
   */
  def updateEndpoint(old: Endpoint, updated: Endpoint): Option[String] = {
    import com.malliina.android.util.PreferenceImplicits._

    val isPlayer = (prefs get Keys.PREF_PLAYER).exists(_ == old.name)
    val isLibrary = (prefs get Keys.PREF_LIBRARY).exists(_ == old.name)
    val others = userAddedEndpoints.filter(_ != old)
    val errorMsg = save(others, updated)
    if (errorMsg.isEmpty) {
      def save(key: String) = prefs.put(key, updated.name)
      if (isPlayer) {
        save(Keys.PREF_PLAYER)
      }
      if (isLibrary) {
        save(Keys.PREF_LIBRARY)
      }
    }
    errorMsg
  }

  protected def save(others: Seq[Endpoint], endpoint: Endpoint): Option[String] = {
    val newName = endpoint.name
    if (others.exists(_.name == newName)) {
      Some(s"An endpoint named $newName already exists. Please rename.")
    } else {
      if (saveEndpoints(others :+ endpoint)) None else Some("Unknown error.")
    }
  }

  def activeEndpoints(prefKeys: String*): Seq[Endpoint] = {
    val names = prefKeys.map(prefs.get).flatten
    endpoints.filter(e => names contains e.name)
  }
}