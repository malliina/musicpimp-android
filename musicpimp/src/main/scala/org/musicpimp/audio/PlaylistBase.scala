package org.musicpimp.audio

trait PlaylistBase {
  /**
   * @return the playlist index wrapped in an Option, or None if no playlist track is active
   */
  def index: Option[Int]

  def tracks: Seq[Track]

  def add(track: Track)

  def add(tracks: Seq[Track]): Unit =
    tracks foreach add

  def remove(index: Int): Unit

  def currentTrack: Option[Track] =
    index.filter(i => i < tracks.size && i >= 0).map(tracks(_))

  def skip(position: Int)

  /**
   *
   * @return the next track in the playlist, if any
   */
  def next: Option[Track] =
    withIndex(_ + 1)

  /**
   *
   * @return the previous track in the playlist if any, otherwise the first track if any, otherwise None
   */
  def previous: Option[Track] =
    withIndex(i => if (i > 0) i - 1 else 0)

  /**
   * Computes a new playlist position from the supplied function and the current position,
   * and returns the track, if any, at the new position.
   *
   * For example, if the parameter is i => i + 1 and a track exists at index i+1, the track
   * at i+1 is returned.
   *
   * @param f function to apply to the current index
   * @return track at new index
   */
  protected def withIndex(f: Int => Int): Option[Track] =
    index map f flatMap trackAt

  protected def trackAt(index: Int): Option[Track] =
    if (index < tracks.size && index >= 0) Some(tracks(index)) else None
}
