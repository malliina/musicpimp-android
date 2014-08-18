package org.musicpimp.ui.dialogs

import org.musicpimp.R
import org.musicpimp.ui.activities.LocalFolders
import org.musicpimp.util.PimpSettings

/**
 *
 * @author mle
 */
class AddFolderDialog extends FolderDialog(None, R.string.add_folder) {
  lazy val settings = new PimpSettings(helper.prefs)

  def onPositive(path: String): Unit = addFolder(path)

  def addFolder(path: String): Unit = withValidation(path) {
    if (settings.loadFolders contains path) {
      helper.showToast(s"Folder already added: $path")
    } else {
      settings.save(LocalFolders.localFoldersPrefKey, settings.loadFolders :+ path)
    }
  }
}
