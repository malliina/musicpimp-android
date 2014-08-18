package org.musicpimp.ui.dialogs

import android.app.{Dialog, AlertDialog}
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.mle.andro.ui.dialogs.DefaultDialog
import org.musicpimp.R.string._

/**
 *
 * @author mle
 */
abstract class PimpDialog(builder: Option[Bundle] => AlertDialog) extends DialogFragment {
  def this(builder: => AlertDialog) = this(_ => builder)

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = builder(Option(savedInstanceState))
}

class TutorialDialog(message: Int, title: Option[Int]) extends DefaultDialog(message, title, Some(next), Some(tutorial_quit))

class FirstUseWelcomeTutorial extends TutorialDialog(tutorial_welcome, Some(tutorial_welcome_title)) {
  override def onPositive(): Unit = new LocalFoldersTutorial().show(getActivity.getSupportFragmentManager, "tutorial-local")
}

class LocalFoldersTutorial extends TutorialDialog(tutorial_local, Some(tutorial_endpoints_title)) {
  override def onPositive(): Any = new RemoteSourcesTutorial().show(getActivity.getSupportFragmentManager, "tutorial-remote")
}

class RemoteSourcesTutorial extends TutorialDialog(tutorial_remote, Some(tutorial_endpoints_title)) {
  override def onPositive(): Any = new BeamTutorial().show(getActivity.getSupportFragmentManager, "tutorial-musicbeamer")
}

class BeamTutorial extends DefaultDialog(tutorial_beam, Some(tutorial_beam_title), Some(done), None)