package org.musicpimp.ui.dialogs

import android.os.Bundle
import org.musicpimp.R.string._
import org.musicpimp.ui.activities.LocalFolders
import org.musicpimp.util.PimpSettings

/**
 *
 * @author mle
 */
class EditFolderDialog(folderOpt: Option[String])
  extends FolderDialog(folderOpt, submit_changes) {

  private val stateKey = "editedFolder"
  private var editedFolder: Option[String] = folderOpt

  // Dialogs need a noparam constructor, however unused
  def this() = this(None)

  lazy val settings = new PimpSettings(helper.prefs)

  /**
   * Maintains the editedFolder variable which is otherwise lost if the user rotates
   * the screen while editing. Thus this class remembers which folder to update
   * if the user submits changes.
   *
   * @param savedInstanceState state
   */
  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
    // only assigns editedFolder if something has been saved in the state key, otherwise leaves it alone
    val folderMaybe = for {
      state <- Option(savedInstanceState)
      folder <- Option(state getString stateKey)
    } yield folder
    folderMaybe foreach (folder => editedFolder = Some(folder))
  }

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    editedFolder foreach (folder => outState.putString(stateKey, folder))
  }

  def onPositive(path: String): Unit =
    editedFolder.fold(helper.showToast("Unable to save changes. Try again later."))(old => updateFolder(old, path))

  def updateFolder(oldPath: String, newPath: String): Unit = withValidation(newPath) {
    val oldFolders = settings.loadFolders
    val editedItemIndex = oldFolders indexOf oldPath
    settings.save(LocalFolders.localFoldersPrefKey, oldFolders.updated(editedItemIndex, newPath))
  }
}
