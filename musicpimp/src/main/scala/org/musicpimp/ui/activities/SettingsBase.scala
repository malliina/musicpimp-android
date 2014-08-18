package org.musicpimp.ui.activities

import android.content.SharedPreferences
import com.mle.android.util.PreferenceImplicits._
import org.musicpimp.beam.BeamCode
import org.musicpimp.http.AlmostOldEndpoint
import org.musicpimp.http.{OldEndpoint, EndpointTypes, Endpoint}
import org.musicpimp.util.Keys._
import org.musicpimp.util.{Keys, PimpLog}
import play.api.libs.json.JsResult
import play.api.libs.json.Json._
import scala.Some


/**
 *
 * @author mle
 */
class SettingsBase(val prefs: SharedPreferences) {
  def saveEndpoints(endpoints: Seq[Endpoint]) = SettingsBase.saveEndpoints(prefs, endpoints)

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
    import com.mle.android.util.PreferenceImplicits._

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

object SettingsBase extends PimpLog {
  val localEndpoint = Endpoint("local", "This device", "", 0, "", "", EndpointTypes.Local)
  val localEndpointNames = Array[CharSequence](localEndpoint.name)
  val inbuiltEndpointNames = Array[CharSequence](localEndpoint.name, Endpoint.beamName)

  /**
   * Saves the endpoints as JSON in a preference key.
   *
   * @param endpoints endpoints to save
   * @return true if the endpoints were written to persistent storage, false otherwise
   */
  def saveEndpoints(prefs: SharedPreferences, endpoints: Seq[Endpoint]): Boolean = prefs.edit()
    .putString(PREF_ENDPOINTS, stringify(obj(PREF_ENDPOINTS -> toJson(endpoints))))
    .commit()

  def endpoints(prefs: SharedPreferences): Seq[Endpoint] =
    Seq(SettingsBase.localEndpoint, Endpoint.fromBeamCode(loadBeamCode(prefs))) ++ userAddedEndpoints(prefs)

  def userAddedEndpoints(prefs: SharedPreferences) =
    (prefs get PREF_ENDPOINTS)
      .flatMap(deserializeOpt(prefs, _))
      .getOrElse(Seq.empty[Endpoint])
      .filter(_.endpointType != EndpointTypes.MusicBeamer)

  /**
   * Deserializes `str` to a sequence of [[Endpoint]]s.
   *
   * This method is backwards compatible with previous [[Endpoint]] versions. If it loads an old endpoint format, the
   * loaded endpoints are converted to the new format and immediately saved as a side-effect. The saving is due to the
   * fact that the new [[Endpoint]] format has a random ID, created during the conversion, which should never change
   * once created.
   *
   * @param prefs prefs, needed in order to save the endpoints, if the loaded ones were of an old format
   * @param str endpoints JSON
   * @return the endpoints
   */
  private def deserialize(prefs: SharedPreferences, str: String): JsResult[Seq[Endpoint]] = {
    val json = parse(str) \ PREF_ENDPOINTS
    val firstAttempt = json.validate[Seq[Endpoint]]
    val saveAfterLoad = firstAttempt.asOpt.isEmpty
    val actual = firstAttempt
      .orElse(json.validate[Seq[AlmostOldEndpoint]].map(_.map(_.toEndpoint)))
      .orElse(json.validate[Seq[OldEndpoint]].map(_.map(_.toEndpoint)))
    actual.filter(_ => saveAfterLoad).foreach(endpoints => saveEndpoints(prefs, endpoints))
    actual
  }

  private def deserializeOpt(prefs: SharedPreferences, json: String): Option[Seq[Endpoint]] =
    deserialize(prefs, json).asOpt

  def loadBeamCode(prefs: SharedPreferences): BeamCode =
    prefs.get(PREF_BEAM)
      .flatMap(str => (parse(str) \ PREF_BEAM).asOpt[BeamCode])
      .getOrElse(BeamCode.default)

}
