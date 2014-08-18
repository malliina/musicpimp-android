package org.musicpimp.local

import collection.mutable
import org.musicpimp.audio._
import scala.Some

/**
 *
 * @author mle
 */
trait LocalPlaylist extends PlaylistBase {
  protected def fireEvent(event: PlayerEvent): Unit

  val playlist = mutable.Buffer.empty[Track]

  def tracks: Seq[Track] = playlist

  private var idx: Option[Int] = None

  def index_=(newIndex: Option[Int]) {
    idx = newIndex
    fireEvent(PlaylistIndexChanged(newIndex))
  }

  def index = idx

  def set(track: Track) {
    playlist.clear()
    add(track)
    index = Some(0)
  }

  def add(track: Track) {
    playlist += track
    fireEvent(PlaylistModified(tracks))
  }

  def add(folder: Folder) {

  }

  def remove(position: Int) {
    playlist remove position
    // if the removed track was "above" the currently playing track, the current index is decremented by one
    index.foreach(i => {
      if (position <= i && i >= 0) {
        index = Some(i - 1)
      }
    })
    fireEvent(PlaylistModified(tracks))
  }

  /**
   * If there is a next track, it is returned and the current playlist
   * index is incremented by one.
   *
   * @return the next track, if any
   */
  def toNext: Option[Track] = {
    val ret = next
    next.foreach(_ => index = index.map(_ + 1))
    ret
  }

  /**
   * If there is a previous track, it is returned and the current playlist
   * index is decremented by one if positive.
   *
   * @return
   */
  def toPrevious: Option[Track] = {
    val ret = previous
    previous.foreach(_ => index = index.map(i => if (i > 0) i - 1 else 0))
    ret
  }
}
