package org.musicpimp.ui.activities

import java.io.IOException
import java.net.UnknownHostException

import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.View
import android.widget.EditText
import com.mle.android.exceptions.{ExplainedException, ExplainedHttpException}
import com.mle.android.http.{HttpConstants, Protocols}
import com.mle.android.ui.Implicits.action2clickListener
import com.mle.util.Utils.executionContext
import org.apache.http.client.HttpResponseException
import org.apache.http.conn.{ConnectTimeoutException, HttpHostConnectException}
import org.musicpimp.http.{Endpoint, EndpointTypes}
import org.musicpimp.network.PimpWifiHelpers
import org.musicpimp.pimp.PimpLibrary
import org.musicpimp.subsonic.SubsonicLibrary
import org.musicpimp.util.{Keys, PimpLog, PimpSettings}
import org.musicpimp.{R, TR, TypedResource}
import play.api.libs.json.JsResultException

import scala.concurrent.TimeoutException

/**
 *
 * @author mle
 */
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

  override protected def onCreate2(savedInstanceState: Option[Bundle]) {
    testButton setOnClickListener ((v: View) => testClicked(v))
    submitButton setOnClickListener ((v: View) => endpointSubmitted(v))
    patient = for {
      bundle <- extras
      endpointName <- Option(bundle.getString(Keys.ENDPOINT))
      endpoint <- settings.endpoints.find(_.name == endpointName)
    } yield endpoint
    patient.foreach(populateFields)
    activityHelper.findView(TR.active_check) setChecked !patient.isDefined
  }

  def endpointSubmitted(view: View): Unit = errorHandled {
    val endpoint = buildEndpointFromInput(patient.map(_.id))
    val saveFunc: Endpoint => Option[String] =
      patient.fold((e: Endpoint) => settings.addEndpoint(e))(p => (e: Endpoint) => settings.updateEndpoint(p, e))
    settings.saveEndpoint(endpoint, saveFunc, activityHelper.findView(TR.active_check).isChecked)
      .foreach(errorMessage => activityHelper.showToast(errorMessage))
    finish()
  }

  def testClicked(view: View): Unit =
    errorHandled(testEndpoint(buildEndpointFromInput(None)))

  def errorHandled[T](f: => T): Unit =
    try {
      f
    } catch {
      case pe: ExplainedException =>
        showFeedbackText(pe.getMessage)
      case e: Exception =>
        showFeedbackText(s"An error occurred. ${e.getMessage}")
    }

  def testEndpoint(endpoint: Endpoint): Unit = {
    val maybeSession = endpoint.endpointType match {
      case EndpointTypes.MusicPimp => Some(new PimpLibrary(endpoint))
      case EndpointTypes.Subsonic => Some(new SubsonicLibrary(endpoint))
      case other => None
    }
    maybeSession.foreach(session => {
      val endpointName = endpoint.endpointType.toString
      showFeedbackText("Testing...", showProgress = true)
      session.ping
        .map(version => showFeedbackText(s"$endpointName ${version.version} at your service."))
        .recover(testFailedFeedback andThen (msg => showFeedbackText(msg)))
        .onComplete(_ => session.close())
    })
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

  val testFailedFeedback: PartialFunction[Throwable, String] = {
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
      "Unable to connect. Unable to resolve host."
    case _: ConnectTimeoutException =>
      "Unable to connect. Timed out."
    case hhce: HttpHostConnectException =>
      warn(hhce.getMessage, hhce)
      "Unable to connect"
    case ioe: IOException =>
      "Unable to connect."
    case jre: JsResultException =>
      warn("JSON failure", jre)
      "The format of the response was unexpected."
    case t: Throwable =>
      failMessage(t, stackTrace = false)
  }

  def populateFields(endpoint: Endpoint) {
    endpoint.endpointType match {
      case EndpointTypes.MusicPimp =>
        setEndTypeRadios(pimp = true, sub = false)
      case EndpointTypes.Subsonic =>
        setEndTypeRadios(pimp = false, sub = true)
      case _ =>
        setEndTypeRadios(pimp = false, sub = false)
    }
    setText(TR.end_name, endpoint.name)
    setText(TR.end_host, endpoint.host)
    setText(TR.end_port, endpoint.port.toString)
    setText(TR.end_username, endpoint.username)
    setText(TR.end_password, endpoint.password)
    setProtocolRadios(ssl = endpoint.protocol == Protocols.Https)
  }

  def setEndTypeRadios(pimp: Boolean, sub: Boolean) {
    findView(TR.musicpimp_radio).setChecked(pimp)
    findView(TR.subsonic_radio).setChecked(sub)
  }

  def setProtocolRadios(ssl: Boolean) {
    findView(TR.http_radio) setChecked !ssl
    findView(TR.https_radio) setChecked ssl
  }

  def buildEndpointFromInput(idOpt: Option[String]): Endpoint = {
    val isPimp = findView(TR.musicpimp_radio).isChecked
    val endType = if (isPimp) EndpointTypes.MusicPimp else EndpointTypes.Subsonic
    val name = readText(TR.end_name)
    val host = readText(TR.end_host)
    val port = readText(TR.end_port).toInt
    val user = readText(TR.end_username)
    val pass = readText(TR.end_password)
    val ssl = findView(TR.https_radio).isChecked
    val protocol = if (ssl) Protocols.Https else Protocols.Http
    val id = idOpt getOrElse Endpoint.newID
    Endpoint(id, name, host, port, user, pass, endType, PimpWifiHelpers.ssid(this, host), protocol = protocol)
  }

  private def readText(id: TypedResource[_ <: EditText]) =
    Option(findView(id).getText.toString).filter(_.length > 0)
      .getOrElse(throw new ExplainedException(s"Please verify that all the fields are filled in properly."))

  private def setText(id: TypedResource[_ <: EditText], text: String): Unit =
    findView(id) setText text
}
