package org.musicpimp.ui.dialogs

import android.app.AlertDialog.Builder
import android.app.{Activity, Dialog, ProgressDialog}
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.text.InputType
import android.widget.EditText
import com.mle.andro.ui.dialogs.DefaultDialog
import com.mle.android.exceptions.UnauthorizedHttpException
import com.mle.android.ui.ActivityUtils
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.R
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.http.Endpoint
import org.musicpimp.pimp.PimpLibrary
import org.musicpimp.ui.activities.EditEndpointActivity
import org.musicpimp.util.PimpSettings
import play.api.libs.json.Json

class EndpointDialog(e: Option[Endpoint])
  extends EditTextDialog(titleRes = Some(R.string.scan_complete)) {

  def this(e: Endpoint) = this(Some(e))

  def this() = this(None)


  private val stateKey = "scannedEndpoint"
  private var endpoint = e

  private var parentActivity: Option[Activity] = None

  lazy val helper = new ActivityHelper(activity)
  lazy val settings = new PimpSettings(helper.prefs)

  def activity = parentActivity getOrElse getActivity

  protected def msg = endpoint.fold("Please supply the password to connect.")(e => s"Found a MusicPimp server at ${e.httpBaseUri}. Please enter the password.")

  override def onPositive(pass: String): Unit = {
    endpoint.map(_.copy(password = pass)).map(verifyCredentials)
  }

  /**
   *
   * @param end
   * @return false if the credentials are invalid
   */
  def verifyCredentials(end: Endpoint): Unit = {
    var progressDialog: Option[ProgressDialog] = None
    helper.onUiThread {
      progressDialog = Some(ProgressDialog.show(getActivity, "Connecting", "Verifying credentials..."))
    }
    val library = new PimpLibrary(end)
    val test =
      library.ping.map(_ => {
        settings.saveEndpoint(end, settings.addEndpoint, activate = true)
      }).recover {
        case _: UnauthorizedHttpException =>
          val act = activity.asInstanceOf[FragmentActivity]
          new InvalidPasswordDialog().show(act.getSupportFragmentManager, "pass-fail")
        case t: Throwable =>
          val act = activity.asInstanceOf[FragmentActivity]
          new GenericErrorAddManuallyDialog().show(act.getSupportFragmentManager, "generic-error")
      }
    test.onComplete(_ => {
      library.close()
      progressDialog.foreach(_.dismiss())
    })
  }

  override def decorate(view: EditText): Unit =
    view setInputType (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)

  override def buildHelp(builder: Builder): Unit = {
    builder setMessage msg
  }

  override def onSaveInstanceState(outState: Bundle): Unit = {
    super.onSaveInstanceState(outState)
    outState.putString(stateKey, Json.stringify(Json.toJson(endpoint)))
  }

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    restoreState(savedInstanceState)
    super.onCreateDialog(savedInstanceState)
  }

  def restoreState(savedInstanceState: Bundle): Unit = {
    for {
      state <- Option(savedInstanceState)
      json <- Option(state getString stateKey)
      end <- Json.parse(json).asOpt[Endpoint]
    } {
      endpoint = Some(end)
    }
  }

  override def onAttach(activity: Activity): Unit = {
    super.onAttach(activity)
    // http://developer.android.com/guide/components/fragments.html#EventCallbacks
    parentActivity = Some(activity)
  }

  override def onDetach(): Unit = {
    // I suppose parentActivity should be emptied?
    super.onDetach()
  }
}

class AddEndpointManuallyDialog
  extends DefaultDialog(R.string.scan_no_results, Some(R.string.scan_complete), Some(R.string.manual_configuration)) with ActivityUtils {
  override def activity: Activity = getActivity

  override def onPositive(): Any = navigate(classOf[EditEndpointActivity])
}
