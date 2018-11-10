package org.musicpimp.local

import java.io.File
import org.musicpimp.audio.MultiLibrary
import org.musicpimp.pimp.PimpLibrary

class MultiFolderLocalLibrary(folders: Seq[File]) extends LocalLibraryBase with MultiLibrary {
  val subLibraries = folders.filter(f => f.isDirectory && f.exists()).map(new LocalLibrary(_))

  def findPath(id: String): Option[File] = {
    val path = PimpLibrary.pathFromId(id)
    subLibraries.view.map(_.findPath(path)).find(_.isDefined).flatten
  }
}

object MultiFolderLocalLibrary {
  val defaultFolders = Seq(
    LocalLibrary.appInternalMusicDir,
    LocalLibrary.publicMusicDir)
  //  ,LocalLibrary.subsonicMusicDir)
  val defaultFolderPaths = defaultFolders map (_.getAbsolutePath)
}
