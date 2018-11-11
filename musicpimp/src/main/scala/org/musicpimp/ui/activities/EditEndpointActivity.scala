package org.musicpimp.ui.activities

import java.io.IOException
import java.net.UnknownHostException

import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.text.{Editable, TextWatcher}
import android.view.View
import android.widget.{EditText, RadioButton}
import com.malliina.android.exceptions.{ExplainedException, ExplainedHttpException}
import com.malliina.android.http.{HttpConstants, Protocols}
import com.malliina.android.ui.Implicits.action2clickListener
import com.malliina.concurrent.ExecutionContexts.cached
import cz.msebera.android.httpclient.client.HttpResponseException
import cz.msebera.android.httpclient.conn.{ConnectTimeoutException, HttpHostConnectException}
import org.musicpimp.http.{Endpoint, EndpointTypes}
import org.musicpimp.network.PimpWifiHelpers
import org.musicpimp.pimp.PimpLibrary
import org.musicpimp.subsonic.SubsonicLibrary
import org.musicpimp.ui.Implicits.fun2checkedChangeListener
import org.musicpimp.util.{Keys, PimpLog, PimpSettings}
import org.musicpimp.{R, TR, TypedResource}
import play.api.libs.json.JsResultException

import scala.concurrent.TimeoutException

class EditEndpointActivity
  extends ActionBarActivity
    with LayoutBaseActivity
    with PimpLog {

  lazy val settings = new PimpSettings(activityHelper.prefs)

  case class EditInfo(endpoint: Endpoint, isLibrary: Boolean, isPlayer: Boolean)

  private var patient: Option[Endpoint] = None

  override val contentView = R.layout.edit_endpoint

  def testButton = findView(TR.test_endpoint_button)

  def submitButton = findView(TR.submit_endpoint_button)

  def testingProgressBar = findView(TR.testingProgressBar)

  def cloudText = tryFindView(TR.end_cloud)

  def cloudTextLabel = tryFindView(TR.end_cloud_text)

  def nameText = tryFindView(TR.end_name)

  def nameTextLabel = tryFindView(TR.end_name_text)

  def hostText = tryFindView(TR.end_host)

  def hostTextLabel = tryFindView(TR.end_host_text)

  def portText = tryFindView(TR.end_port)

  def portTextLabel = tryFindView(TR.end_port_text)

  def pimpRadio = findView(TR.musicpimp_radio)

  def cloudRadio = findView(TR.cloud_radio)

  def subsonicRadio = findView(TR.subsonic_radio)

  def protocolGroup = tryFindView(TR.protocol_radio_group)

  def protocolLabel = tryFindView(TR.protocol_text)

  override protected def onCreate2(savedInstanceState: Option[Bundle]) {
    testButton setOnClickListener ((v: View) => testClicked(v))
    submitButton setOnClickListener ((v: View) => endpointSubmitted(v))
    arrangeOnChecked(pimpRadio, isCloud = false)
    arrangeOnChecked(cloudRadio, isCloud = true)
    arrangeOnChecked(subsonicRadio, isCloud = false)

    def arrangeOnChecked(radio: RadioButton, isCloud: Boolean) =
      radio.setOnCheckedChangeListener((isChecked: Boolean) => if (isChecked) arrangeViews(isCloud))

    patient = for {
      bundle <- extras
      endpointName <- Option(bundle.getString(Keys.ENDPOINT))
      endpoint <- settings.endpoints.find(_.name == endpointName)
    } yield endpoint
    patient.foreach(populateFields)
    findView(TR.active_check) setChecked patient.isEmpty
    arrangeViews(cloudRadio.isChecked)
    // syncs the name with the cloud ID if necessary
    cloudText.foreach(_.addTextChangedListener(new TextWatcher {
      var updateName = false

      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {
        updateName = nameText.exists(_.getText.toString == s.toString)
      }

      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = ()

      override def afterTextChanged(s: Editable): Unit = {
        if (updateName) {
          val text = s.toString
          nameText.foreach(name => name setText text)
        }
      }
    }))
  }

  def arrangeViews(isCloud: Boolean) = {
    val (nonCloudVis, cloudVis, focusWinner) =
      if (isCloud) (View.GONE, View.VISIBLE, cloudText)
      else (View.VISIBLE, View.GONE, nameText)
    adjustVisibility(nonCloudVis, hostText, hostTextLabel, portText, portTextLabel, protocolGroup, protocolLabel)
    adjustVisibility(cloudVis, cloudText, cloudTextLabel)
    focusWinner.foreach(_.requestFocus())
  }

  def adjustVisibility(visibility: Int, views: Option[View]*) = onUiThread {
    views.flatten.foreach(_ setVisibility visibility)
  }

  def endpointSubmitted(view: View): Unit = errorHandled {
    val endpointEither = buildEndpointFromInput(patient.map(_.id))
    foldFeedback(endpointEither, endpoint => {
      val saveFunc: Endpoint => Option[String] =
        patient.fold((e: Endpoint) => settings.addEndpoint(e))(p => (e: Endpoint) => settings.updateEndpoint(p, e))
      settings.saveEndpoint(endpoint, saveFunc, activityHelper.findView(TR.active_check).isChecked)
        .foreach(errorMessage => activityHelper.showToast(errorMessage))
      finish()
    })
  }

  def testClicked(view: View): Unit =
    errorHandled {
      foldFeedback(buildEndpointFromInput(None), testEndpoint)
    }

  def foldFeedback(either: Either[String, Endpoint], f: Endpoint => Unit) =
    either.fold(err => showFeedbackText(err), f)

  def errorHandled[T](f: => T): Unit =
    try {
      f
    } catch {
      case pe: ExplainedException =>
        showFeedbackText(pe.getMessage)
      case e: Exception =>
        warn("Edit endpoint failed.", e)
        showFeedbackText(s"An error occurred. ${e.getMessage}")
    }

  def testEndpoint(endpoint: Endpoint): Unit = {
    val maybeSession = endpoint.endpointType match {
      case EndpointTypes.MusicPimp => Some(new PimpLibrary(endpoint))
      case EndpointTypes.Cloud => Some(new PimpLibrary(endpoint))
      case EndpointTypes.Subsonic => Some(new SubsonicLibrary(endpoint))
      case _ => None
    }
    maybeSession.foreach { session =>
      val endpointName = endpoint.endpointType.toString
      showFeedbackText("Testing...", showProgress = true)
      session.ping
        .map(version => showFeedbackText(s"$endpointName ${version.version} at your service."))
        .recover(testFailedFeedback(endpoint) andThen (msg => showFeedbackText(msg)))
        .onComplete(_ => session.close())
    }
  }

  def setTestingProgressVisibility(visibility: Int) =
    onUiThread(testingProgressBar setVisibility visibility)

  def showFeedbackText(feedback: String, showProgress: Boolean = false): Unit = onUiThread {
    val feedbackView = findView(TR.testFeedback)
    feedbackView setText feedback
    feedbackView setVisibility View.VISIBLE
    val progressVisibility = if (showProgress) View.VISIBLE else View.GONE
    testingProgressBar setVisibility progressVisibility
  }

  def testFailedFeedback(endpoint: Endpoint): PartialFunction[Throwable, String] = {
    case ehe: ExplainedHttpException =>
      ehe.reason
    case pe: ExplainedException =>
      pe.getMessage
    case toe: TimeoutException =>
      "Timed out."
    case hre: HttpResponseException =>
      val errorCode = hre.getStatusCode
      errorCode match {
        case HttpConstants.UNAUTHORIZED =>
          "Unauthorized. Check your credentials."
        case HttpConstants.NOT_FOUND =>
          "The remote resource was not found."
        case _ =>
          s"A network error occurred. HTTP error $errorCode."
      }
    case _: UnknownHostException =>
      s"Unable to connect to ${endpoint.httpBaseUri}. Unable to resolve host."
    case _: ConnectTimeoutException =>
      s"Unable to connect to ${endpoint.httpBaseUri}. Timed out."
    case hhce: HttpHostConnectException =>
      warn(hhce.getMessage, hhce)
      s"Unable to connect to ${endpoint.httpBaseUri}."
    case ioe: IOException =>
      s"Unable to connect to ${endpoint.httpBaseUri}."
    case jre: JsResultException =>
      warn("JSON failure", jre)
      "The format of the response was unexpected."
    case t: Throwable =>
      failMessage(t, stackTrace = false)
  }

  def populateFields(endpoint: Endpoint) {
    endpoint.endpointType match {
      case EndpointTypes.MusicPimp =>
        setEndTypeRadios(pimp = true, cloud = false, sub = false)
      case EndpointTypes.Cloud =>
        setEndTypeRadios(pimp = false, cloud = true, sub = false)
      case EndpointTypes.Subsonic =>
        setEndTypeRadios(pimp = false, cloud = false, sub = true)
      case _ =>
        setEndTypeRadios(pimp = false, cloud = false, sub = false)
    }
    endpoint.cloudID.foreach(id => setText(TR.end_cloud, id))
    setText(TR.end_name, endpoint.name)
    setText(TR.end_host, endpoint.host)
    setText(TR.end_port, endpoint.port.toString)
    setText(TR.end_username, endpoint.username)
    setText(TR.end_password, endpoint.password)
    setProtocolRadios(ssl = endpoint.protocol == Protocols.Https)
  }

  def setEndTypeRadios(pimp: Boolean, cloud: Boolean, sub: Boolean) {
    findView(TR.musicpimp_radio).setChecked(pimp)
    findView(TR.cloud_radio).setChecked(cloud)
    findView(TR.subsonic_radio).setChecked(sub)
  }

  def setProtocolRadios(ssl: Boolean) {
    findView(TR.http_radio) setChecked !ssl
    findView(TR.https_radio) setChecked ssl
  }

  def buildEndpointFromInput(idOpt: Option[String]): Either[String, Endpoint] = {
    import org.musicpimp.http.EndpointTypes.{Cloud, MusicPimp, Subsonic}
    val endType =
      if (findView(TR.musicpimp_radio).isChecked) MusicPimp
      else if (findView(TR.cloud_radio).isChecked) Cloud
      else Subsonic
    if (endType == EndpointTypes.Cloud) {
      for {
        cloudID <- readText(TR.end_cloud)
        name <- readText(TR.end_name)
        user <- readText(TR.end_username)
        pass <- readText(TR.end_password)
      } yield {
        Endpoint.forCloud(idOpt, name, cloudID, user, pass)
      }
    } else {
      for {
        name <- readText(TR.end_name)
        host <- readText(TR.end_host)
        port <- readText(TR.end_port).map(_.toInt).right
        user <- readText(TR.end_username)
        pass <- readText(TR.end_password)
      } yield {
        val ssl = findView(TR.https_radio).isChecked
        val protocol = if (ssl) Protocols.Https else Protocols.Http
        val id = idOpt getOrElse Endpoint.newID
        val isPimp = endType == EndpointTypes.MusicPimp
        Endpoint(id, name, host, port, user, pass, endType, None, PimpWifiHelpers.ssid(this, host).filter(_ => isPimp), protocol = protocol, autoSync = isPimp)
      }
    }
  }

  private def readText(id: TypedResource[_ <: EditText]): Either.RightProjection[String, String] =
    tryFindView(id).flatMap(editText => Option(editText.getText.toString)).filter(_.length > 0).map(Right.apply)
      .getOrElse(Left("Please verify that all the fields are filled in properly.")).right

  private def setText(id: TypedResource[_ <: EditText], text: String): Unit =
    findView(id) setText text
}
