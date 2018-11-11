package org.musicpimp.ui.activities

import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.ContextMenu.ContextMenuInfo
import android.view.{ContextMenu, Menu, MenuItem, View}
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.{AdapterView, ArrayAdapter, ListView}
import com.malliina.android.ui.Implicits.action2itemClickListener2
import com.malliina.concurrent.ExecutionContexts.cached
import org.musicpimp.{R, TR}

import scala.concurrent.Future

/** Base activity for managing a list of items: add/remove/edit/update.
  *
  * @tparam T type of item
  */
trait ItemsManager[T] extends ActionBarActivity with LayoutBaseActivity {
  def optionsMenuLayout: Int

  def addLabel: Int

  def loadAdapterAsync(): Future[ArrayAdapter[T]]

  def addClicked(): Unit

  def onItemSelected(item: T, position: Int): Unit

  def remove(item: T): Unit

  def tryFindListView = Option(findListView)

  def findListView = activityHelper.findView(TR.items_list)

  override val contentView = R.layout.items_activity

  override protected def onCreate2(savedInstanceState: Option[Bundle]) {
    val listView = findListView
    listView.setOnItemClickListener((av: AdapterView[_], i: Int) => onItemSelected(av, i))
    registerForContextMenu(listView)
  }

  override def onResume() {
    super.onResume()
    populateList(findListView)
  }

  // action bar
  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(optionsMenuLayout, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case R.id.add_item =>
      addClicked()
      true
    case _ =>
      super.onOptionsItemSelected(item)
  }

  // long-click menu
  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    //    menu.findItem(R.id.remove_item) setTitle addLabel
    getMenuInflater.inflate(R.menu.items_context, menu)
  }

  override def onContextItemSelected(item: MenuItem): Boolean = {
    def menuInfo = item.getMenuInfo.asInstanceOf[AdapterContextMenuInfo]

    val position = menuInfo.position
    item.getItemId match {
      case R.id.remove_item if position > 0 =>
        val item = itemAt[T](menuInfo.position, TR.items_list)
        remove(item)
        populateList(findListView)
        true
      case _ =>
        super.onContextItemSelected(item)
    }
  }

  /** Assigns an appropriate adapter to `listView`.
    *
    * @param listView list to populate
    */
  def populateList(listView: ListView): Unit =
    loadAdapterAsync().foreach(a => onUiThread(findListView setAdapter a))

  def onItemSelected(av: AdapterView[_], index: Int): Unit = {
    val item = av.getItemAtPosition(index).asInstanceOf[T]
    onItemSelected(item, index)
  }
}
