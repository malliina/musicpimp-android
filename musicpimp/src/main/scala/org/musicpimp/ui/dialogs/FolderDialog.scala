package org.musicpimp.ui.dialogs

import android.app.AlertDialog.Builder
import java.io.File
import org.musicpimp.R
import org.musicpimp.andro.ui.ActivityHelper

/**
 * This class is abstract because DialogFragments must have a default no-param constructor.
 *
 */
abstract class FolderDialog(oldPath: Option[String], positiveLabel: Int)
  extends EditTextDialog(positiveLabel, oldPath, messageRes = Some(R.string.add_folder_help), hint = R.string.folder_hint) {
  lazy val helper = new ActivityHelper(getActivity)

  def onPositive(input: String)

  override def buildHelp(builder: Builder): Unit = builder setMessage R.string.add_folder_help

  protected def withValidation[T](path: String)(f: => T): Unit = {
    val file = new File(path)
    if (!file.isDirectory) helper.showToast(s"Not a valid directory: $path")
    else if (!file.canRead) helper.showToast(s"Cannot read directory: $path")
    else f
  }
}



