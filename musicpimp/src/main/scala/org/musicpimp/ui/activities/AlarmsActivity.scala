package org.musicpimp.ui.activities

import android.app.Activity
import android.os.Bundle
import android.view.{Menu, View}
import android.widget.{ArrayAdapter, Spinner}
import com.malliina.android.messaging.MessagingException
import com.malliina.android.util.PreferenceImplicits.RichPrefs
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import org.musicpimp.andro.messaging.IMessagingUtils
import org.musicpimp.andro.util.Implicits.RichBundle
import org.musicpimp.audio.{LibraryManager, PlayerManager}
import org.musicpimp.http.{Endpoint, EndpointTypes}
import org.musicpimp.pimp.Alarms.Alarm
import org.musicpimp.pimp.AlarmsClient
import org.musicpimp.ui.Implicits.fun2checkedChangeListener
import org.musicpimp.ui.SpinnerHelper
import org.musicpimp.ui.adapters.AlarmsAdapter
import org.musicpimp.util.{Keys, PimpLog, PimpSettings}
import org.musicpimp.{PimpApp, R, TR, TypedResource}

import scala.concurrent.Future

class AlarmsActivity extends ItemsManager[Alarm] with PimpLog {

  lazy val alarmSpinner = new AlarmSpinner(this)

  lazy val settingsHelper = new PimpSettings(activityHelper.prefs)

  private val errorOccurredMessage = "An error occurred."

  override val contentView: Int = R.layout.alarms

  private var currentEndpoint: Option[Endpoint] = None

  def emptyAdapter: ArrayAdapter[Alarm] = new AlarmsAdapter(this, Seq.empty)

  def gcmToggleView = findView(TR.gcmToggle)

  override def findListView = findView(TR.items_list)

  def feedbackView = findView(TR.alarm_feedback)

  def progressView = findView(TR.alarms_loading_bar)

  override def optionsMenuLayout: Int = R.menu.add_menu

  override def addLabel: Int = R.string.add

  override protected def onCreate2(bundle: Option[Bundle]): Unit = {
    super.onCreate2(bundle)
    if (isMessagingEnabled) {
      gcmToggleView.setOnCheckedChangeListener((isChecked: Boolean) => selectedEndpoint.foreach(e => {
        if (isChecked) registerMessaging(e) else unregisterMessaging(e)
      }))
    }
    currentEndpoint =
      for {
        e <- extras
        endpointID <- e findString Keys.ENDPOINT
        endpoint <- settingsHelper.endpoints.find(_.id == endpointID)
      } yield endpoint
  }

  def isMessagingEnabled = PimpApp.appStoreInfo.supportsMessaging

  override def onResume(): Unit = {
    super.onResume()
    alarmSpinner.reload()
    if (alarmSpinner.spinnerChoices.size == 0) {
      hideViews()
      setFeedback("No MusicPimp endpoint has been added. First add one, then come back to configure its alarms.")
    } else {
      showViews()
    }
    selectedEndpoint foreach updateUI
  }

  override def onItemSelected(item: Alarm, position: Int): Unit = {
    currentEndpoint.foreach(e => {
      navigate(classOf[EditAlarmActivity], Keys.ENDPOINT -> e.id, Keys.ALARM_ID -> item.id)
    })
    //    currentEndpoint.map(_.id).fold(warn("Cannot navigate, no endpoint is configured"))(endId => {
    //      activityHelper.navigate(classOf[EditAlarmActivity], Keys.ENDPOINT -> endId, Keys.ALARM_ID -> item.id)
    //    })
  }

  override def remove(item: Alarm): Unit = selectedEndpoint
    .foreach(e => withClient(e, _.remove(this, item.id)).map(_ => updateUI(e)))

  override def addClicked(): Unit = activityHelper.navigate(classOf[EditAlarmActivity])

  override def loadAdapterAsync(): Future[ArrayAdapter[Alarm]] = {
    selectedEndpoint.fold(Future.successful(emptyAdapter))(endpoint => {
      withClient(endpoint, _.alarms.map(as => new AlarmsAdapter(this, as)))
    })
  }

  def selectedEndpoint: Option[Endpoint] = alarmSpinner.spinnerView
    .flatMap(s => Option(s.getSelectedItem).map(_.asInstanceOf[String]))
    .flatMap(name => settingsHelper.endpoints.find(_.name == name))

  override def onCreateOptionsMenu(menu: Menu): Boolean = false

  def updateUI(endpoint: Endpoint): Unit = {
    currentEndpoint = Some(endpoint)
    hideFeedback()
    // TODO validate version, required >= 2.3.5, set feedback if lower
    if (isMessagingEnabled) {
      isRegistered(endpoint).map(isGcmEnabled => {
        onUiThread(gcmToggleView setChecked isGcmEnabled)
      })
    }
    withProgress {
      loadAdapterAsync().map(a => {
        onUiThread {
          findListView setAdapter a
          if (a.getCount == 0) {
            setFeedback("No alarms have been added. To add one, select a track from the library and choose 'Schedule playback'.")
          }
        }
        showViews()
      }).recoverAll(t => {
        warn(errorOccurredMessage, t)
        hideViews()
        val explanation = Option(t.getMessage) getOrElse ""
        setFeedback(s"$errorOccurredMessage $explanation")
      })
    }
  }

  def withProgress[T](task: Future[T]): Future[T] = {
    if (!task.isCompleted) {
      showProgress()
    }
    task.onComplete(_ => hideProgress())
    task
  }

  def showViews() = updateVisibility(visible = true)

  def hideViews() = updateVisibility(visible = false)

  def updateVisibility(visible: Boolean): Unit = activityHelper.onUiThread {
    val visibility = if (visible) View.VISIBLE else View.INVISIBLE
    uiElements.foreach(_.setVisibility(visibility))
  }

  def uiElements = Seq(findListView) ++ (if (isMessagingEnabled) Seq(gcmToggleView) else Seq.empty[View])

  private def withClient[T](endpoint: Endpoint, f: AlarmsClient => Future[T]): Future[T] = {
    val client = new AlarmsClient(endpoint)
    val result = f(client)
    result.onComplete(_ => client.close())
    result
  }

  private def registerMessaging(endpoint: Endpoint): Unit = {
    withMessaging(endpoint, messaging => {
      messaging.tryRegister(this)
        .map(id => info(s"Registered for push notifications with device ID: $id"))
        .recoverAll(t => warn(errorOccurredMessage, t))
    })
  }

  private def unregisterMessaging(endpoint: Endpoint) = withMessaging(endpoint, _.unregister(this))

  private def isRegistered(endpoint: Endpoint) = withMessaging(endpoint, _.isRegistered(this))

  private def withMessaging[T](endpoint: Endpoint, f: IMessagingUtils => Future[T]): Future[T] = {
    PimpApp.appStoreInfo.pushUtils.fold(Future.failed[T](new MessagingException(s"Cannot use messaging as it's not supported.")))(builder => {
      val utils = builder(endpoint)
      val result = f(utils)
      result.onComplete(_ => utils.close())
      result
    })
  }

  private def setFeedback(feedback: String) = activityHelper.onUiThread {
    val view = feedbackView
    view setText feedback
    view setVisibility View.VISIBLE
  }

  private def hideFeedback() = hideView(feedbackView)

  private def showProgress() = showView(progressView)

  private def hideProgress() = hideView(progressView)

  def showView[T <: View](viewResource: TypedResource[T]): Unit = showView(activityHelper.findView(viewResource))

  def hideView[T <: View](viewResource: TypedResource[T]): Unit = hideView(activityHelper.findView(viewResource))

  def showView[T <: View](view: T): Unit = setVisibility(view, View.VISIBLE)

  def hideView[T <: View](view: T): Unit = setVisibility(view, View.GONE)

  //  private def setVisibility[T <: View](view: TypedResource[T], visibility: Int): Unit =
  //    setVisibility(findView(view), visibility)

  private def setVisibility[T <: View](view: T, visibility: Int): Unit =
    activityHelper.onUiThread(view setVisibility visibility)

  class AlarmSpinner(activity: Activity) extends SpinnerHelper(activity) with PimpLog {

    override def spinnerChoices: Seq[String] =
      settingsHelper.userAddedEndpoints.filter(e => e.endpointType == EndpointTypes.MusicPimp || e.endpointType == EndpointTypes.Cloud).map(_.name)

    override def initialSpinnerSelection(choices: Seq[String]): Option[String] = {
      def pref(key: String) = activityHelper.prefs.get(key).filter(choices.contains)
      currentEndpoint.map(_.name).filter(choices.contains) orElse
        pref(PlayerManager.prefKey) orElse
        pref(LibraryManager.prefKey) orElse
        choices.headOption
    }


    override def onSpinnerItemSelected(endpointName: String): Unit = {
      settingsHelper.userAddedEndpoints.find(_.name == endpointName)
        .fold(warn(s"Unable to find endpoint with name: $endpointName"))(updateUI)
    }

    override def spinnerResource: TypedResource[Spinner] = TR.alarm_player_spinner
  }

}