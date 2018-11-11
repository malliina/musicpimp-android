package org.musicpimp.ui.activities

import android.app.Activity
import android.content.SharedPreferences
import android.hardware.Camera
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBar.Tab
import android.support.v7.app.{ActionBar, ActionBarActivity}
import android.view._
import android.widget.Spinner
import com.mle.android.receivers.DownloadEventReceiver
import com.mle.android.util.AndroUtils
import com.mle.android.util.PreferenceImplicits.RichPrefs
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.PimpApp.AppStores
import org.musicpimp.audio.PlayStates.PlayState
import org.musicpimp.audio._
import org.musicpimp.beam.BeamCode
import org.musicpimp.http.Endpoint
import org.musicpimp.local.LocalLibrary
import org.musicpimp.network.{DownloadSettings, EndpointScanner}
import org.musicpimp.ui.SpinnerHelper
import org.musicpimp.ui.activities.MainActivity.isFirstActivityCreate
import org.musicpimp.ui.adapters.PimpPagerAdapter
import org.musicpimp.ui.dialogs._
import org.musicpimp.usage.PimpUsageController
import org.musicpimp.util.{Keys, Messaging, PimpLog}
import org.musicpimp.{PimpApp, R, TR, TypedResource}

class MainActivity
  extends ActionBarActivity
  with MessageToasting
  with PreferenceListeningActivity
  with PimpLog {

  override val contentView = R.layout.root

  lazy val spinnerHelper = new MainSpinnerHelper(this)
  lazy val backgroundManager = new BackgroundManager(this)

  // we keep the options menu so we can update the icon of the play/pause button in response to events
  var optionsMenu: Option[Menu] = None
  val trackListener = new PimpTrackListener

  def player = PlayerManager.active

  def loadUnlimitedPlaybackAllowed: Boolean = prefs.getBoolean(Keys.PREF_UNLIMITED_PLAYBACK, false)

  override protected def onCreate2(savedInstanceState: Option[Bundle]) {
    initCustomTopActionBar(getSupportActionBar)
    val layoutConfig = getResources.getString(R.string.layout_name)
    initSwipeTabs(layoutConfig)
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    initApp()
  }

  private def initApp() = {
    val isFirstUse = prefs.getBoolean(Keys.PREF_FIRST_USE, true)
    if (isFirstUse) {
      prefs.putBoolean(Keys.PREF_FIRST_USE, value = false)
      onFirstAppStart()
    }
    if (isFirstActivityCreate) {
      isFirstActivityCreate = false
      onFirstActivityCreate()
    }
  }

  private def onFirstAppStart() = {
    new FirstUseWelcomeTutorial().show(getSupportFragmentManager, "tutorial-welcome")
  }

  private def onFirstActivityCreate() = {
    val appStoreInfo = PimpApp.appStoreInfo
    debug(s"Starting app, built for appstore: ${appStoreInfo.appStore}")
    PimpUsageController.allowUnlimited = loadUnlimitedPlaybackAllowed
    //    info(s"Allow unlimited: ${PimpUsageController.allowUnlimited}")
    // Check if package is built for samsung apps and if so, whether package com.sec.android.iap exists first.
    // If the package does not exist, a popup is displayed. Is this desired? emulator throws an exception then
    // when the popup is dismissed.
    if (appStoreInfo.appStore != AppStores.Samsung || AndroUtils.isPackageinstalled(this, "com.sec.android.iap"))
      appStoreInfo.iapUtils.syncPremiumStatus(this) //.recoverAll(t => warn(s"Unable to sync purchase status", t))
    LocalLibrary.maintainCache(prefs)
    DownloadSettings.load()
  }

  override def onPlaybackLimitExceeded(): Unit = new IapDialog().show(getSupportFragmentManager, "in-app dialog")

  override def onResume() {
    super.onResume()
    spinnerHelper.reload()
    trackListener.registerPlayerEventsListener()
    backgroundManager.setBackground(player.currentTrack)
    startListening(player)
  }

  override def onPause() {
    trackListener.unregisterPlayerEventsListener()
    stopListening(player)
    super.onPause()
  }

  def onTrackChanged(trackOpt: Option[Track]): Unit = {
    backgroundManager.setBackground(trackOpt)
  }

  def onPlayStateChanged(state: PlayStates.PlayState): Unit =
    optionsMenu.foreach(menu => updatePlayPauseIcon(menu, state))


  def startListening(p: Player): Unit = {
    p.startPolling()
    optionsMenu.foreach(updatePlaybackButtons)
  }

  def updatePlaybackButtons(menu: Menu): Unit = {
    updatePlayPauseIcon(menu, player.status.state)
    Seq(R.id.action_previous, R.id.action_next)
      .foreach(id => menu.findItem(id) setEnabled player.supportsSeekAndSkip)
  }

  def stopListening(p: Player): Unit = p.stopPolling()

  def initSwipeTabs(layoutConfig: String) {
    val pager = findView(TR.rootPager)
    val pageAdapter = new PimpPagerAdapter(getSupportFragmentManager, combinedPlayerAndPlaylist = layoutConfig != "default")
    pager setAdapter pageAdapter

    val actionBar = getSupportActionBar
    actionBar setNavigationMode ActionBar.NAVIGATION_MODE_TABS

    // shows correct tab after the user has clicked on one
    val listener = new ActionBar.TabListener {
      def onTabSelected(tab: Tab, ft: FragmentTransaction) {
        pager setCurrentItem tab.getPosition
      }

      def onTabUnselected(tab: Tab, ft: FragmentTransaction) {}

      def onTabReselected(tab: Tab, ft: FragmentTransaction) {}

    }
    def addTab(title: String, selected: Boolean = false) {
      actionBar.addTab(actionBar.newTab().setText(title).setTabListener(listener), selected)
    }
    pageAdapter.tabs.foreach(t => addTab(t.title, selected = t.selected))

    // shows the correct tab when the user swipes
    pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      override def onPageSelected(position: Int) {
        actionBar setSelectedNavigationItem position
      }
    })
  }

  def initCustomTopActionBar(actionBar: ActionBar) {
    // DISPLAY_SHOW_HOME renders the logo and ensures the action bar is above the tabs
    actionBar setDisplayOptions (ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM)
    actionBar setCustomView R.layout.action_top
  }


  def onSpinnerItemSelected(endpointName: String): Unit = {
    if (endpointName == Endpoint.beamName) {
      val dialog =
        if (Camera.getNumberOfCameras == 0) {
          new NoCameraDialog
        } else if (SettingsBase.loadBeamCode(prefs) == BeamCode.default) {
          new FirstTimeBeamDialog
        } else {
          new ReturningBeamDialog
        }
      dialog.show(getSupportFragmentManager, "beam")
    } else {
      PlayerManager.activate(endpointName)
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    optionsMenu = Some(menu)
    getMenuInflater.inflate(R.menu.action_menu, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    updatePlaybackButtons(menu)
    super.onPrepareOptionsMenu(menu)
  }

  private def updatePlayPauseIcon(menu: Menu, state: PlayStates.PlayState) {
    import org.musicpimp.R.drawable._
    val icon =
      if (state == PlayStates.Started || state == PlayStates.Playing) ic_media_pause
      else ic_media_play
    val playPauseMenuButton = menu.findItem(R.id.action_play_pause)
    onUiThread(playPauseMenuButton setIcon icon)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case R.id.action_play_pause =>
      activityHelper.trueToastException(player.playOrPause())
    case R.id.action_next =>
      activityHelper.trueToastException(player.playNext())
    case R.id.action_previous =>
      activityHelper.trueToastException(player.playPrevious())
    case R.id.action_search =>
//      navigate(classOf[SearchableActivity])
      onSearchRequested()
      true
    case R.id.action_refresh =>
      //      val dialog: Option[ProgressDialog] = None
      //      onUiThread(Some(ProgressDialog.show(this, "Refreshing", "Looking for MusicPimp servers...")))
      LibraryManager.active.invalidateCache()
      EndpointScanner.syncWlanEndpoints(this).onComplete(_ => {
        //        dialog.foreach(_.dismiss())
        Messaging.reload(silent = false)
      })
      true
    case R.id.action_open_downloads =>
      DownloadEventReceiver.openDownloadsActivity(this)
      true
    case R.id.action_open_prefs =>
      navigate(classOf[SettingsActivity])
      true
    case R.id.action_about =>
      navigate(classOf[AboutActivity])
      true
    //    case R.id.action_test =>
    //      navigate(classOf[TestActivity])
    //      true
    case _ =>
      super.onOptionsItemSelected(item)
  }

  override def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String): Unit = {
    if (key == Keys.PREF_ENDPOINTS) {
      spinnerHelper.reload()
    }
  }

  class PimpTrackListener extends TrackListener {
    override def onTrackChanged(trackOpt: Option[Track]): Unit = backgroundManager.setBackground(trackOpt)

    override def onPlayStateChanged(state: PlayState): Unit = optionsMenu.foreach(menu => updatePlayPauseIcon(menu, state))

    override def onActivated(player: Player): Unit = startListening(player)

    override def onDeactivated(player: Player): Unit = stopListening(player)
  }

  class MainSpinnerHelper(activity: Activity) extends SpinnerHelper(activity) {
    override def spinnerChoices: Seq[String] = SettingsBase.endpoints(prefs).map(_.name)

    override def spinnerResource: TypedResource[Spinner] = TR.player_spinner

    override def onSpinnerItemSelected(endpointName: String): Unit = {
      if (endpointName == Endpoint.beamName) {
        val dialog =
          if (Camera.getNumberOfCameras == 0) {
            new NoCameraDialog
          } else if (SettingsBase.loadBeamCode(prefs) == BeamCode.default) {
            new FirstTimeBeamDialog
          } else {
            new ReturningBeamDialog
          }
        dialog.show(getSupportFragmentManager, "beam")
      } else {
        PlayerManager.activate(endpointName)
      }
    }

    override def initialSpinnerSelection(choices: Seq[String]): Option[String] = {
      prefs.get(PlayerManager.prefKey).filter(choices.contains)
    }
  }

}

object MainActivity {
  var isFirstActivityCreate = true
}
