package org.musicpimp.ui

import android.app.{ProgressDialog, Activity}
import android.support.v4.app.{FragmentActivity, DialogFragment}
import com.mle.android.network.WifiHelpers
import com.mle.util.Utils.executionContext
import org.musicpimp.R
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.network.EndpointScanner
import org.musicpimp.ui.activities.EditEndpointActivity
import org.musicpimp.ui.dialogs.{AddEndpointManuallyDialog, MessageBoxDialog, EndpointDialog}
import scala.Some
import scala.concurrent._
import scala.util.Try

/**
 * @author Michael
 */
class EndpointHelper(activity: Activity) extends ActivityHelper(activity) {
  private var progressDialog: Option[ProgressDialog] = None

  def addClicked(): Unit = navigate(classOf[EditEndpointActivity])

  def startScan(): Unit = {
    if (WifiHelpers.isWifiConnected(activity)) {
      onUiThread {
        progressDialog = Some(ProgressDialog.show(activity, "Scanning", "Looking for MusicPimp servers..."))
      }
      // If this future completes before the progress dialog is shown,
      // the dialog will eventually be shown, then never dismissed.
      // TODO: Figure out how to manage ProgressDialogs properly.
      EndpointScanner.searchEndpoint(activity)
        .map(e => showDialog(new EndpointDialog(e), "password_dialog"))
        .recover(scanFailHandler)
        .onComplete(_ => dismissProgressDialog())
    } else {
      showDialog(new MessageBoxDialog(
        message = R.string.scan_requires_wifi,
        title = Some(R.string.wifi_requred)), "wifi_required")
    }
  }

  def dismissProgressDialog() {
    progressDialog.foreach(_.dismiss())
    progressDialog = None
  }

  private val scanFailHandler: PartialFunction[Throwable, Unit] = {
    case _: TimeoutException =>
      //      info("Scan timed out")
      suggestManualConfiguration()
    case _: Throwable =>
      suggestManualConfiguration()
  }

  private def suggestManualConfiguration(): Unit =
    showDialog(new AddEndpointManuallyDialog(), "manual_add")

  // wraps in Try, because of: IllegalStateException: Can not perform this action after onSaveInstanceState
  // cast is safe as long as the activity is an ActionBarActivity, which is true for all usages atm
  private def showDialog(d: DialogFragment, tag: String) =
    onUiThread(Try(d.show(activity.asInstanceOf[FragmentActivity].getSupportFragmentManager, tag)))
}
