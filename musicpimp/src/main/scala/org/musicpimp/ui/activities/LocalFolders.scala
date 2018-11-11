package org.musicpimp.ui.activities

import android.content.{Context, SharedPreferences}
import com.malliina.andro.ui.adapters.EditIconOneLineAdapter
import org.musicpimp.R
import org.musicpimp.ui.activities.LocalFolders.{FolderAdapter, localFoldersPrefKey}
import org.musicpimp.ui.dialogs.{AddFolderDialog, EditFolderDialog}
import org.musicpimp.util.PimpSettings
import scala.concurrent.Future

class LocalFolders
  extends ItemsManager[String]
  with PreferenceListeningActivity {

  lazy val settings = new PimpSettings(prefs)

  override val optionsMenuLayout: Int = R.menu.add_menu

  override val addLabel: Int = org.musicpimp.R.string.add_folder

  override def loadAdapterAsync() = Future.successful(new FolderAdapter(this, settings.loadFolders))

  override def onItemSelected(folder: String, position: Int): Unit = {
    if (position > 0) {
      new EditFolderDialog(Some(folder)).show(getSupportFragmentManager, "edit_folder")
    }
  }

  override def addClicked(): Unit =
    new AddFolderDialog().show(getSupportFragmentManager, "add_folder")

  def remove(item: String): Unit = settings.save(localFoldersPrefKey, settings.loadFolders filter (_ != item))

  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    if (key == localFoldersPrefKey) {
      tryFindListView foreach populateList
    }
  }
}

object LocalFolders {
  val localFoldersPrefKey = "folders"

  class FolderAdapter(ctx: Context, folders: Seq[String]) extends EditIconOneLineAdapter(ctx, folders) {
    override def noEditImageResource: Int = R.drawable.holofolder
  }

}
