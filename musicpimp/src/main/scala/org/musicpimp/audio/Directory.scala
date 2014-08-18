package org.musicpimp.audio

/**
 *
 * @author mle
 */
case class Directory(folders: Seq[Folder], tracks: Seq[Track]) {
  def ++(other: Directory) = Directory(
    (folders ++ other.folders).distinct.sortBy(_.title.toLowerCase),
    (tracks ++ other.tracks).distinct.sortBy(_.title.toLowerCase)
  )

  def addDistinct(other: Directory) = {
    val distinctTracks = other.tracks.filter(t => tracks.find(track => track.path == t.path || track.title == t.title).isEmpty)
    //    info("" + folders)
    //    info("" + other.folders)
    Directory(
      (folders ++ other.folders).distinct.sortBy(_.title.toLowerCase),
      tracks ++ distinctTracks
    )
  }

  val isEmpty = folders.isEmpty && tracks.isEmpty
}

object Directory {
  val empty = Directory(Seq.empty, Seq.empty)
}
