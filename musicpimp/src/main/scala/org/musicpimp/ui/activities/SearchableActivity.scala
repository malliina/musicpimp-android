package org.musicpimp.ui.activities

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu.ContextMenuInfo
import android.view.{ContextMenu, MenuItem, View}
import android.widget.{ListAdapter, AdapterView, AbsListView, ArrayAdapter}
import com.mle.android.ui.Implicits.action2itemClickListener2
import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.audio.LibraryManager
import org.musicpimp.ui.MusicActions
import org.musicpimp.ui.adapters.{DownloadProgress, SearchAdapter, TrackItem}
import org.musicpimp.util.PimpLog
import org.musicpimp.{R, TR}

import scala.concurrent.Future

/**
 * @author Michael
 * @see http://developer.android.com/guide/topics/search/search-dialog.html#SearchableActivity
 */
class SearchableActivity extends LayoutBaseActivity with MusicDownloadUpdatingActivity with PimpLog {
  lazy val actions = new MusicActions(activityHelper)

  override def contentView: Int = R.layout.search

  def findListView = findView(TR.listView).asInstanceOf[AbsListView]

  def tryFindListView = activityHelper.tryFindView[AbsListView](R.id.listView)

  def adapterOpt[T] = tryFindListView.flatMap(l => Option(l.getAdapter).map(_.asInstanceOf[T]))

  def findProgressBar = findView(TR.loadingBar)

  def findFeedbackView = findView(TR.feedbackText)

  override protected def onCreate2(state: Option[Bundle]): Unit = {
    val listView = findListView
    listView.setOnItemClickListener(actions.onItemSelected _)
    registerForContextMenu(listView)
    handleIntent(getIntent)
  }

  override def onNewIntent(intent: Intent): Unit = {
    setIntent(intent)
    handleIntent(intent)
  }

  def handleIntent(intent: Intent): Unit = {
    if (intent.getAction == Intent.ACTION_SEARCH) {
      val query = intent.getStringExtra(SearchManager.QUERY)
      search(query)
    }
  }

  def search(term: String): Unit = {
    info(s"Searching: $term")
    withProgressAndFeedback(term) {
      LibraryManager.active.search(term)
        .map(_.map(TrackItem(_, DownloadProgress.empty)))
        .map(new SearchAdapter(this, _))
    }
  }

  def withProgressAndFeedback[T](ctx: String)(loadItems: => Future[ArrayAdapter[T]]) = {
    import android.view.View.{GONE, VISIBLE}
    val feedback = findFeedbackView
    val progressBar = findProgressBar
    onUiThread {
      feedback setVisibility GONE
      progressBar setVisibility VISIBLE
    }
    loadItems.map(adapter => {
      onUiThread {
        progressBar setVisibility GONE
        findListView.asInstanceOf[AdapterView[ListAdapter]] setAdapter adapter
        if (adapter.isEmpty) {
          feedback setVisibility VISIBLE
          feedback setText emptyFeedback(ctx)
        } else {
          feedback setVisibility GONE
        }
        onSearchRequested()
      }
    }).recoverAll(t => {
      onUiThread {
        feedback setText "An error occurred."
        feedback setVisibility VISIBLE
        progressBar setVisibility GONE
        onSearchRequested()
      }
      warn("Search failure", t)
    })
  }

  def emptyFeedback(ctx: String): String = s"No results for: $ctx"

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    actions.inflate(menu, getMenuInflater)
  }

  override def onContextItemSelected(item: MenuItem): Boolean = {
    actions.onContextItemSelected(item, findListView, i => super.onContextItemSelected(i))
  }
}
