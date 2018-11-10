package org.musicpimp.ui.activities

import BeamScanActivity._
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.musicpimp.beam.BeamCode
import org.musicpimp.http.Endpoint
import org.musicpimp.util.Keys
import org.musicpimp.{TR, R}
import play.api.libs.json.Json._

/** Note that app Barcode Scanner runs in landscape mode, so the screen
  * is rotated immediately when the intent is started, causing `onCreate`
  * to be called twice if we're in portrait mode. But the intent should
  * only be launched once not twice, therefore the `scanning` variable
  * is used to ensure only one instance of the scanner is opened.
  *
  * http://code.google.com/p/zxing/wiki/ScanningViaIntent
  */
class BeamScanActivity extends LayoutBaseActivity {
  // orientation changes
  private var ended = false
  private var feedbackRes: Option[Int] = None

  def findFeedback = activityHelper.findView(TR.`beam_help`)

  override val contentView = R.layout.beam

  override protected def onCreate2(state: Option[Bundle]): Unit = {
    val hasEnded = state.map(_.getBoolean(SCAN_ENDED)).filter(_ == true).isDefined
    ended = hasEnded
    state.map(_.getInt(FEEDBACK)).filter(_ > 0).foreach(res => {
      feedbackRes = Some(res)
      try {
        findFeedback setText res
      } catch {
        // Not sure why this exception is occasionally thrown. Not of critical importance anyway.
        // android.content.res.Resources$NotFoundException: String resource ID #0x0
        // Update: The filter above may have fixed it.
        case re: RuntimeException =>
        //          warn("Unable to set feedback for BeamScanActivity", re)
      }
    })
    scanIfNotAlreadyScanning()

  }

  def scanIfNotAlreadyScanning() {
    if (!ended && !scanning) {
      scanning = true
      // onActivityResult is called once this activity quits
      activityHelper.navigateForResult(classOf[ScannerActivity], SCAN_REQUEST)
    }
  }

  /**
    * Parses the scanned barcode, sets up the MusicBeamer player then finishes
    * this activity. Displays a feedback message to the user if she fucks up.
    *
    * @param requestCode will be SCAN_REQUEST, see above method
    * @param resultCode  should be RESULT_OK if all goes well
    * @param data        data that contains the scanned barcode string in key SCAN_RESULT
    */
  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    scanning = false
    if (requestCode == SCAN_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        Option(data getStringExtra SCAN_RESULT).fold(setFeedback(R.string.beam_nothing_scanned))(text => {
          parse(text).asOpt[BeamCode].fold(setFeedback(R.string.beam_format_error))(code => {
            saveCodeAndSetPlayer(code)
            finish()
          })
        })
      } else {
        setFeedback(R.string.beam_nothing_scanned)
      }
    }
  }

  def saveCodeAndSetPlayer(code: BeamCode): Unit = {
    // saves the MusicBeamer code for possible future use and sets the player to MusicBeamer
    prefs.edit()
      .putString(Keys.PREF_BEAM, stringify(obj(Keys.PREF_BEAM -> toJson(code))))
      .putString(Keys.PREF_PLAYER, Endpoint.beamName)
      .apply()
  }


  def setFeedback(textResource: Int) {
    ended = true
    feedbackRes = Some(textResource)
    findFeedback setText textResource
  }

  override def onSaveInstanceState(outState: Bundle) {
    outState putBoolean(SCAN_ENDED, ended)
    feedbackRes foreach (res => outState putInt(FEEDBACK, res))
    super.onSaveInstanceState(outState)
  }
}

object BeamScanActivity {
  val SCAN_REQUEST = 42
  val SCAN_RESULT = "org.musicpimp.scan.result"
  private val SCAN_ENDED = "org.musicpimp.scan.ended"
  private val FEEDBACK = "org.musicpimp.scan.feedback"
  private var scanning: Boolean = false
}

