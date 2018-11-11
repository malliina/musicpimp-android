package org.musicpimp.ui.activities

import android.content.SharedPreferences
import android.view.MenuItem
import org.musicpimp.R
import org.musicpimp.http.{EndpointTypes, Endpoint}
import org.musicpimp.ui.EndpointHelper
import org.musicpimp.ui.adapters.EndpointAdapter
import org.musicpimp.util.{PimpSettings, Keys}
import scala.concurrent.Future

class Endpoints
  extends ItemsManager[Endpoint]
  with PreferenceListeningActivity {

  lazy val settingsHelper = new PimpSettings(prefs)
  lazy val endpointHelper = new EndpointHelper(this)

  override def addClicked(): Unit = navigate(classOf[EditEndpointActivity]) // endpointHelper.addClicked()

  def loadUnlimitedPlaybackAllowed: Boolean = prefs.getBoolean(Keys.PREF_UNLIMITED_PLAYBACK, false)

  override val optionsMenuLayout: Int = R.menu.remote_endpoints_menu

  val addLabel: Int = R.string.add_endpoint

  override def onPause(): Unit = {
    endpointHelper.dismissProgressDialog()
    super.onPause()
  }

  override def loadAdapterAsync() = Future.successful(new EndpointAdapter(this, settingsHelper.endpoints drop 1))

  def remove(item: Endpoint): Unit = settingsHelper.saveEndpoints(settingsHelper.userAddedEndpoints.filter(_ != item))

  def onItemSelected(item: Endpoint, position: Int): Unit = {
    // disallows editing of the local device (index 0) and the MusicBeamer endpoint
    if (position > 0 && item.endpointType != EndpointTypes.MusicBeamer) {
      navigate(classOf[EditEndpointActivity], Keys.ENDPOINT -> item.name)
    }
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case R.id.scan_endpoints =>
      endpointHelper.startScan()
      true
    case _ =>
      super.onOptionsItemSelected(item)
  }

  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    if (key == Keys.PREF_ENDPOINTS) {
      tryFindListView foreach populateList
    }
  }
}
