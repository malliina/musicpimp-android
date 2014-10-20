package org.musicpimp.ui.fragments

import PlaylistControls._
import android.graphics.Color
import android.os.{Parcelable, Bundle}
import android.view.ContextMenu.ContextMenuInfo
import android.view._
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.{TextView, ListView, AdapterView}
import com.mle.android.ui.Implicits.action2itemClickListener2
import org.musicpimp.audio._
import org.musicpimp.ui.adapters.TrackItem
import org.musicpimp.ui.adapters.{DownloadProgress, LibraryItemAdapter}
import org.musicpimp.{R, TR}

/**
 *
 * @author mle
 */
trait PlaylistControls extends PlaybackFragment with MusicDownloadUpdatingFragment {
  def layoutId: Int

  override def activity = getActivity

  // scroll position maintenance
  var listState: Option[Parcelable] = None

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(layoutId, container, false)

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
    initViews(Option(savedInstanceState))
  }

  /**
   * Called when the [[android.app.Activity]] this fragment belongs to has been created.
   *
   * @param state state
   */
  def initViews(state: Option[Bundle]): Unit =
    findListView.foreach(listView => {
      listView.setOnItemClickListener((_: AdapterView[_], index: Int) => player skip index)
      registerForContextMenu(listView)
      listState = state.flatMap(s => Option(s.getParcelable[Parcelable](LIST_STATE)))
      showPlaylistItems(listView, player.tracks)
    })

  override def onResume() {
    super.onResume()
    resetUI()
  }

  override def onPause() {
    unsubscribeFromPlayerEvents()
    super.onPause()
  }

  /**
   * Called when the player has changed or the fragment is resumed. This means both the player and the playlist should
   * be redrawn.
   */
  def resetUI(): Unit = {
    if (latestPlayer != player) {
      latestPlayer = player
      onPlayerChanged()
    }
    redraw()
    subscribeToPlayerEvents()
  }

  def redraw(): Unit = highlightPlaylistItem(playlistIndex)

  def onPlayerChanged() {
    findListView.foreach(listView => {
      activityHelper.onUiThread {
        // ensures that out of date items are not shown while loading new items
        listView setAdapter null
        // loads playlist items
        showPlaylistItems(listView, player.tracks)
      }
    })
  }

  override def onSaveInstanceState(outState: Bundle) {
    // saves scroll position, which will be available in subsequent calls to onCreate(Bundle)
    // if the fragment has not been visited, the View will not be found and find returns null
    findListView.map(listView => outState.putParcelable(LIST_STATE, listView.onSaveInstanceState()))
    super.onSaveInstanceState(outState)
  }

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    activity.getMenuInflater.inflate(R.menu.playlist_item_context, menu)
  }

  override def onContextItemSelected(item: MenuItem): Boolean = {
    def menuInfo = item.getMenuInfo.asInstanceOf[AdapterContextMenuInfo]
    activityHelper.toastOnException(default = true) {
      item.getItemId match {
        case R.id.remove_from_playlist =>
          // saves list position
          listState = findListView.flatMap(list => Option(list.onSaveInstanceState()))
          player remove menuInfo.position
          true
        case _ =>
          super.onContextItemSelected(item)
      }
    }
  }

  def onPlayerChanged(newPlayer: Player): Unit = {
    subscribeToPlayerEvents()
  }

  protected def highlightPlaylistItem(index: Option[Int]) {
    // When the adapter items are decorated, the item matching
    // the current playlist index is automatically highlighted.
    // So when the index has changed, we just redecorate the items
    // for the correct item to be highlighted.
    activityHelper.onUiThread(adapterOpt[LibraryItemAdapter].foreach(_.notifyDataSetChanged()))
  }

  def showPlaylistItems(listView: ListView, items: Seq[Track]) {
    val trackItems = items.map(TrackItem(_, DownloadProgress.empty))
    findFeedbackView.map(feedback => {
      activityHelper.onUiThread {
        feedback setText R.string.loading
        feedback setVisibility View.VISIBLE
      }
    })
    val adapter = new LibraryItemAdapter(getActivity, Seq.empty, trackItems) {
      override def decorate(view: View, item: MusicItem, position: Int): Unit = {
        super.decorate(view, item, position)
        val color = playlistIndex.filter(_ == position).fold(DEFAULT_COLOR)(_ => HIGHLIGHT_COLOR)
        findView(view, TR.firstLine.id).asInstanceOf[TextView] setTextColor color
      }
    }
    activityHelper.onUiThread {
      listView setAdapter adapter
      findFeedbackView.foreach(feedback => {
        if (items.isEmpty) {
          feedback setText R.string.empty_playlist
        } else {
          feedback setVisibility View.GONE
        }
      })

      listState.foreach(listView.onRestoreInstanceState)
      listState = None
    }
  }

  def playlistIndex = player.index // player.status.playlistIndex

  def findListView = activityHelper.tryFindView(TR.playlist)

  def adapterOpt[T]: Option[T] =
    findListView.flatMap(l => Option(l.getAdapter).map(_.asInstanceOf[T]))

  def findFeedbackView = activityHelper.tryFindView(TR.playlist_feedback)

  def player = PlayerManager.active
}

object PlaylistControls {
  val LIST_STATE = "list_state"
  // android.R.color.holo_blue_light
  val HIGHLIGHT_COLOR = Color.CYAN
  // I tested.
  val DEFAULT_COLOR = -4276546
}
