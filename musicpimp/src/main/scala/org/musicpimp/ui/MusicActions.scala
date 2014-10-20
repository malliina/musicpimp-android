package org.musicpimp.ui

import java.io.File

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.view.{ContextMenu, MenuInflater, MenuItem}
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.{AbsListView, AdapterView}
import com.mle.android.http.{HttpConstants, HttpUtil}
import com.mle.util.Utils
import com.mle.util.Utils.executionContext
import org.musicpimp.R
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.audio._
import org.musicpimp.http.EndpointTypes
import org.musicpimp.network.DownloadSettings
import org.musicpimp.ui.activities.EditAlarmActivity
import org.musicpimp.ui.adapters.TrackItem
import org.musicpimp.util.{Keys, PimpLog}

/**
 * @author Michael
 */
class MusicActions(utils: ActivityHelper) extends PimpLog{
  def downloadManager = utils.activity.getSystemService(Context.DOWNLOAD_SERVICE).asInstanceOf[DownloadManager]

  def library = LibraryManager.active

  def player = PlayerManager.active

  def itemAt[T](index: Int, list: AbsListView): T = list.getAdapter.getItem(index).asInstanceOf[T]

  def inflate(menu: ContextMenu, inflater: MenuInflater) = {
    val isPimp = LibraryManager.activeEndpoint(utils.prefs).endpointType == EndpointTypes.MusicPimp
    val menuResource = if (isPimp) R.menu.track_context else R.menu.track_context_no_scheduling
    inflater.inflate(menuResource, menu)
  }

  def onContextItemSelected(item: MenuItem, list: AbsListView, fallBack: MenuItem => Boolean): Boolean = {
    def menuInfo = item.getMenuInfo.asInstanceOf[AdapterContextMenuInfo]
    utils.toastOnException(default = true) {
      val musicItem = itemAt[MusicItem](menuInfo.position, list)
      item.getItemId match {
        case R.id.play_selected =>
          musicItem match {
            case TrackItem(track, _) =>
              play(track)
            case folder: Folder =>
              val tracksFuture = library tracksIn folder
              tracksFuture.filter(_.size > 0) foreach (tracks => {
                play(tracks.head)
                add(tracks.tail)
              })
          }
          true
        case R.id.add_playlist =>
          musicItem match {
            case TrackItem(track, _) =>
              add(track)
            case folder: Folder =>
              val tracksFuture = library tracksIn folder
              tracksFuture foreach add
          }
          true
        case R.id.download =>
          musicItem match {
            case TrackItem(track, _) =>
              downloadIfNotExists(track)
            case folder: Folder =>
              //              info(s"Downloading folder: ${folder.title}")
              library.tracksIn(folder).map(downloadIfNotExists).onFailure {
                case e: Exception => utils.showToast(s"An error occurred. ${e.getMessage}")
              }
          }
          true
        case R.id.schedule_playback =>
          musicItem match {
            case TrackItem(track, _) =>
              val e = LibraryManager.activeEndpoint(utils.prefs)
              if (e.endpointType == EndpointTypes.MusicPimp) {
                utils.navigate(classOf[EditAlarmActivity],
                  Keys.ENDPOINT -> e.id,
                  Keys.TRACK_ID -> track.id,
                  Keys.TRACK_TITLE -> track.title)
              } else {
                utils.showToast("Set your MusicPimp server as the active music library first.")
              }
            case _ =>
              utils.showToast("Please select a track instead.")
          }
          true
        case _ =>
          fallBack(item)
      }
    }
  }
  def onItemSelected(av: AdapterView[_], index: Int) {
    val item = av getItemAtPosition index
    utils.toastOnException(()) {
      item match {
        case TrackItem(track, _) =>
          play(track)
      }
    }
  }

  def play(track: Track): Unit = withDownload(track, player.setAndPlay)

  def add(track: Track): Unit = withDownload(track, player.add)

  private def withDownload(t: Track, f: Track => Unit): Unit = {
    f(t)
    if (player.isLocal) {
      downloadIfNotExists(t)
    }
  }

  def add(tracks: Seq[Track]) {
    //    info(s"Adding ${tracks.size} tracks to playlist")
    player add tracks
    if (player.isLocal) {
      downloadIfNotExists(tracks)
    }
  }

  def downloadIfNotExists(tracks: Seq[Track]): Seq[Option[Long]] =
    tracks map downloadIfNotExists

  /**
   * Downloads `track` if its source is remote and it does not exist locally.
   *
   * @param track track to download
   * @return A unique ID for the download if it was submitted, None otherwise.
   */
  def downloadIfNotExists(track: Track): Option[Long] = {
    val source = track.source
    val existsLocally = LibraryManager.localLibrary exists track
    //    info(s"Track $track with path: ${track.path} exists locally: $existsLocally")
    if (!existsLocally && source.isAbsolute && (source.getScheme == "http" || source.getScheme == "https")) {
      download(track)
    } else {
      None
    }
  }

  /**
   * Enqueues a request to download `track` to the local device. The download
   * is handled by the download manager app.
   *
   * @param track track to download
   * @return a unique ID for the download
   */
  def download(track: Track): Option[Long] = {
    // creates destination directory
    val destinationFile = new File(DownloadSettings.downloadsDir, track.path)
    Option(destinationFile.getParentFile).map(_.mkdirs())
    val request = new DownloadManager.Request(track.source)
      .setTitle(track.title)
      .setDescription("MusicPimp download")
      .setDestinationUri(Uri fromFile destinationFile)
      .addRequestHeader(HttpConstants.AUTHORIZATION, HttpUtil.authorizationValue(track.username, track.password))
    // only added in API level 11
    //    request.allowScanningByMediaScanner()
    Utils.opt[Long, SecurityException](downloadManager enqueue request)
  }
}