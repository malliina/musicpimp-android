package org.musicpimp.ui.dialogs

import android.app.{AlertDialog, Dialog}
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.mle.android.ui.Implicits.clickListener
import org.musicpimp.R.string._
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.audio.PlayerManager
import org.musicpimp.http.Endpoint
import org.musicpimp.ui.activities.BeamScanActivity

abstract class BeamDialog(message: Int, neg: Int, setOnNegative: Boolean)
  extends DialogFragment {

  lazy val helper = new ActivityHelper(getActivity)

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(getActivity)
    builder.setTitle(action_beam)
      .setMessage(message)
      .setPositiveButton(beam_yes_scan,
        (_: DialogInterface, _: Int) => helper.navigate(classOf[BeamScanActivity]))
      .setNegativeButton(neg,
        (_: DialogInterface, _: Int) =>
          if (setOnNegative)
            helper.prefs.edit().putString(PlayerManager.prefKey, Endpoint.beamName).apply())
    builder.create()
  }
}

class FirstTimeBeamDialog
  extends BeamDialog(beam_opens_scanner_help, beam_cancel, setOnNegative = false)

class ReturningBeamDialog
  extends BeamDialog(beam_choice_help, beam_use_previous, setOnNegative = true)
