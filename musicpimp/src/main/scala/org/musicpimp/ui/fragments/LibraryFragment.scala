package org.musicpimp.ui.fragments

import java.io.IOException

import android.os.{Bundle, Parcelable}
import android.support.v4.app.FragmentActivity
import android.view.ContextMenu.ContextMenuInfo
import android.view.{ContextMenu, MenuItem, View}
import android.widget._
import com.fasterxml.jackson.core.JsonParseException
import com.mle.andro.ui.adapters.{StaticIconOneLineAdapter, TwoPartItem}
import com.mle.android.exceptions.ExplainedHttpException
import com.mle.android.ui.Implicits.action2itemClickListener2
import com.mle.android.ui.fragments.DefaultFragment
import com.mle.concurrent.ExecutionContexts.cached
import org.apache.http.client.HttpResponseException
import org.musicpimp.audio._
import org.musicpimp.ui._
import org.musicpimp.ui.activities._
import org.musicpimp.ui.adapters.{DownloadProgress, LibraryItemAdapter, TrackItem}
import org.musicpimp.ui.dialogs.AddFolderDialog
import org.musicpimp.ui.fragments.LibraryFragment.LIST_STATE
import org.musicpimp.util.{Keys, Messaging, PimpLog}
import org.musicpimp.{R, TR}

/**
 *
 * @author mle
 */
class LibraryFragment
  extends DefaultFragment
  with MusicDownloadUpdatingFragment
  with ReloadListening
  with PimpLog {

  override val layoutId: Int = R.layout.library

  lazy val endpointHelper = new EndpointHelper(getActivity)
  lazy val actions = new MusicActions(endpointHelper)

  private var latestLibrary: MediaLibrary = LibraryManager.active

  def library = LibraryManager.active

  def player = PlayerManager.active

  //  def lv = endpointHelper.findView(TR.)

  def absList = endpointHelper.findView(TR.listView).asInstanceOf[AbsListView]

  def findListView = tryFindView[AbsListView](R.id.listView)

  def findFeedbackView = endpointHelper.findView(TR.feedbackText)

  def findProgressBar = endpointHelper.findView(TR.loadingBar)

  def findHelpListView = endpointHelper.findView(TR.helpListView)

  // scroll position maintenance
  private var listScrollState: Option[Parcelable] = None
  private var folderMeta: Option[FolderMeta] = None
  private var firstResumeSinceCreated = true

  val helpItems = Seq(
    TwoPartItem(android.R.drawable.ic_menu_search, R.string.scan_subtext),
    TwoPartItem(android.R.drawable.ic_menu_add, R.string.add_pc_subtext),
    TwoPartItem(R.drawable.holofolder, R.string.add_local_subtext)
  )

  override def initViews(state: Option[Bundle]) {
    firstResumeSinceCreated = true
    findListView.foreach(listView => {
      listView.setOnItemClickListener(onItemSelected _)
      registerForContextMenu(listView)
      listScrollState = state.flatMap(s => Option(s.getParcelable[Parcelable](LIST_STATE)))
      folderMeta = for {
        folderId <- extras(Keys.FOLDER)
        path <- extras(Keys.PATH)
      } yield FolderMeta(folderId, path)
    })
    initHelpListView()
  }

  def initHelpListView() {
    val helpAdapter = new StaticIconOneLineAdapter(getActivity, R.layout.help_item, helpItems)
    val list = findHelpListView
    list setAdapter helpAdapter
    list.setOnItemClickListener((av: AdapterView[_], index: Int) => {
      val item = (av getItemAtPosition index).asInstanceOf[TwoPartItem]
      item.secondResource match {
        case R.string.scan_subtext =>
          endpointHelper.startScan()
        case R.string.add_pc_subtext =>
          endpointHelper.addClicked()
        case R.string.add_local_subtext =>
          new AddFolderDialog().show(activity.asInstanceOf[FragmentActivity].getSupportFragmentManager, "add_folder")
          Messaging.reload()
      }
    })
  }

  def extras(key: String): Option[String] =
    Option(getActivity.getIntent.getExtras)
      .flatMap(xtras => Option(xtras.getString(key)))

  override def onResume() {
    super.onResume()
    ensureUpToDateLibraryContentIsDisplayed()
  }

  override def onPause(): Unit = {
    endpointHelper.dismissProgressDialog()
    super.onPause()
  }

  def onReload(silent: Boolean): Unit =
    findListView.foreach(listView => onUiThread {
      val op = if (silent) refreshFolder _ else loadFolder _
      op(listView, folderMeta.map(_.id))
    })

  /**
   * Reloads the library if
   * a) the library has changed since the last time this fragment was resumed or
   * b) this is the first call to onResume since this fragment was created
   *
   * If a), we reload because the user may have changed the library since last time.
   * If b), we reload because although the library may be the same, it may have sent a
   * Reload event while this fragment was destroyed and thus didn't catch, meaning the
   * currently loaded content may still be outdated.
   *
   * @return if a library reload was initiated
   */
  private def ensureUpToDateLibraryContentIsDisplayed(): Boolean = {
    findListView.map(listView => {
      // refreshes and goes to the root folder if the library has changed
      if (latestLibrary != library) {
        firstResumeSinceCreated = false
        latestLibrary = library
        // ensures that music items from the previously selected library are not shown
        // while loading the new folder
        listView.asInstanceOf[AdapterView[ListAdapter]] setAdapter null
        folderMeta = None
        // loads new folder
        showPathAndLoadFolder(listView, folderMeta)
        true
      } else if (firstResumeSinceCreated) {
        firstResumeSinceCreated = false
        showPathAndLoadFolder(listView, folderMeta)
        true
      } else {
        false
      }
    }).filter(_ == true).isDefined
  }

  def adapterOpt[T] = findListView.flatMap(l => Option(l.getAdapter).map(_.asInstanceOf[T]))

  override def onSaveInstanceState(outState: Bundle): Unit = {
    // saves scroll position, will be available in the bundle passed to onCreate(Bundle)
    saveListState.foreach(state => outState.putParcelable(LIST_STATE, state))
    super.onSaveInstanceState(outState)
  }

  def saveListState = findListView.flatMap(l => Option(l.onSaveInstanceState()))

  def showPathAndLoadFolder(listView: AbsListView, folder: Option[FolderMeta]): Unit = onUiThread {
    endpointHelper.findView(TR.pathText) setText folder.fold("")(_.path)
    loadFolder(listView, folder.map(_.id))
  }

  def loadFolder(listView: AbsListView, folderId: Option[String]) = {
    val loadingBar = findProgressBar
    hideFeedbackAndHelp()
    loadingBar setVisibility View.VISIBLE

    refreshFolder(listView, folderId)
  }

  def refreshFolder(listView: AbsListView, folderId: Option[String]): Unit = {
    val contents = folderId.fold(library.rootFolder)(library.folder)
    contents.map(showDirectory(_, listView, folderId))
      .onFailure(constructFeedback andThen showFeedbackAndHideLoading)
  }

  private def showDirectory(dir: Directory, listView: AbsListView, folderId: Option[String]): Unit = {
    val folders = dir.folders
    val trackItems = dir.tracks.map(TrackItem(_, DownloadProgress.empty))
    val adapter = new LibraryItemAdapter(activity, folders, trackItems)
    val feedback = findFeedbackView
    val loadingBar = findProgressBar
    onUiThread {
      loadingBar setVisibility View.GONE
      // AbsListView.setAdapter is only defined in API level 11, however,
      // AbsListView is an AdapterView[ListAdapter], for which setAdapter
      // is defined already in API level 1. So by upcasting we can use
      // setAdapter also for API levels below 11.
      listView.asInstanceOf[AdapterView[ListAdapter]] setAdapter adapter
      if (dir.isEmpty) {
        feedback setVisibility View.VISIBLE
        val isRoot = folderId.isEmpty
        // shows a special, more helpful message if the root folder is empty in
        // which case the user has probably not configured any music source yet
        val (feedbackText, helpVisibility) =
          if (isRoot) (R.string.root_empty, View.VISIBLE)
          else (R.string.empty_folder, View.GONE)
        feedback setText feedbackText
        findHelpListView setVisibility helpVisibility
        // An empty folder may have been returned if the connection to the master library has failed.
        // We don't know from the API, because MultiLibrary.folder(...) never fails. So we ping
        // to check if the (master) library responds.
        library.ping.recover {
          case _: IOException => onUiThread {
            feedback setText R.string.unable_connect_source
          }
        }
      } else {
        hideFeedbackAndHelp()
      }
      listScrollState.foreach(listView.onRestoreInstanceState)
      listScrollState = None
    }
  }

  def hideFeedbackAndHelp() {
    findFeedbackView setVisibility View.GONE
    findHelpListView setVisibility View.GONE
  }

  private val constructFeedback: PartialFunction[Throwable, String] = {
    case jpe: JsonParseException =>
      //      warn(s"A response was received but its content could not be parsed correctly.", jpe)
      "A response was received but its content could not be understood. Ensure that your server is up-to-date and try again."
    case ehe: ExplainedHttpException =>
      ehe.reason
    case hre: HttpResponseException =>
      "A network error occurred."
    case ioe: IOException =>
      "Unable to connect"
    case e: Exception =>
      val explanation = failMessage(e, stackTrace = false)
      warn(s"Unknown error", e)
      s"An error occurred: $explanation"
  }

  private def showFeedbackAndHideLoading(text: String): Unit = onUiThread {
    //    warn(s"Failed to load library: $text")
    findProgressBar setVisibility View.GONE
    val feedback = findFeedbackView
    feedback setText text
    feedback setVisibility View.VISIBLE
  }

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    actions.inflate(menu, activity.getMenuInflater)
  }

  override def onContextItemSelected(item: MenuItem): Boolean = {
    actions.onContextItemSelected(item, absList, i => super.onContextItemSelected(i))
  }

  def onItemSelected(av: AdapterView[_], index: Int) {
    val item = av getItemAtPosition index
    endpointHelper.toastOnException(()) {
      item match {
        case TrackItem(track, _) =>
          actions.play(track)
        case f: Folder =>
          navigate(f)
      }
    }
  }

  def navigate(f: Folder) {
    // Constructs a new folder path from the current folder path. We wish to show the
    // current folder path to the user.
    // Subsonic does not return the path in its JSON responses, and its folder IDs
    // are nondescript integers, so we need to manually keep track of the path of
    // the current folder when navigating its library.
    val newRelativePath = folderMeta.fold(f.title)(meta => s"${meta.path}/${f.title}")
    navigate(classOf[MainActivity], Keys.FOLDER -> f.id, Keys.PATH -> newRelativePath)
  }
}

object LibraryFragment {
  val LIST_STATE = "list_state"
}

case class FolderMeta(id: String, path: String)
