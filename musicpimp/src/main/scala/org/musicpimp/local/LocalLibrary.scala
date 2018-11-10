package org.musicpimp.local

import java.io.File

import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import com.mle.file.DiskHelpers
import com.mle.storage._
import org.musicpimp.PimpApp
import org.musicpimp.audio.{Directory, Folder, Track}
import org.musicpimp.pimp.PimpLibrary
import org.musicpimp.util.{Keys, PimpLog}

import scala.concurrent.Future
import scala.concurrent.duration._

class LocalLibrary(val rootDirectory: File) extends LocalLibraryBase {
  val supportedExtensions = Seq("mp3")
  // + 1 is for the slash ('/') between the end of the root path and beginning of subpaths
  private val rootPrefixLength = rootDirectory.getAbsolutePath.length + 1

  def rootFolder: Future[Directory] = loadFolder("", rootDirectory)

  def folder(id: String): Future[Directory] = {
    val path = PimpLibrary.pathFromId(id)
    val dir = new File(rootDirectory, path)
    if (dir.exists()) loadFolder(path.toString, dir)
    else Future.successful(Directory.empty)
  }

  private def loadFolder(relativePath: String, musicDirectory: File): Future[Directory] = {
    val (dirs, files) = musicDirectory.listFiles().partition(_.isDirectory)
    val tracks = readTracks(files)
    /**
     * We replace '/' with '\\' to be consistent with the way path separators are represented
     * by MusicPimp server 1.8.0, this eases comparisons between folders.
     *
     * TODO: Ensure MusicPimp always returns '/', not '\\' for path separators, then remove
     * the replace call below.
     */
    val folders = dirs.
      filter(containsTracks).
      map(dir => Folder(PimpLibrary.encode(relativize(dir.getPath).replace('/', '\\')), dir.getName))
    Future.successful(Directory(folders, tracks))
  }

  /**
   *
   * @param dir folder to search for tracks
   * @return true if the folder or any of its subfolders contains music tracks, false otherwise
   */
  def containsTracks(dir: File): Boolean = {
    val tagReader = new MediaMetadataRetriever
    val (dirs, files) = dir.listFiles().partition(_.isDirectory)
    files.exists(file => parseTrack(file, tagReader).isDefined) || dirs.exists(dir => containsTracks(dir))
  }

  def tracksIn(dir: File, recursive: Boolean = false): Seq[Track] =
    if (recursive) tracksInDirRecursively(dir)
    else readTracks(dir.listFiles().filter(_.isFile))

  def tracksInDirRecursively(dir: File): Seq[Track] = {
    val paths = dir.listFiles()
    val (dirs, files) = paths.partition(_.isDirectory)
    val shallowTracks = readTracks(files)
    val subTracks = dirs.flatMap(tracksInDirRecursively)
    subTracks ++ shallowTracks
  }

  override def tracksIn(folderId: String): Future[Seq[Track]] =
    Future.successful(tracksInDirRecursively(path(PimpLibrary.pathFromId(folderId))))

  def readTracks(paths: Seq[File]): Seq[Track] = {
    val tagReader = new MediaMetadataRetriever
    paths.flatMap(parseTrack(_, tagReader))
  }

  def parseTrack(file: File, tagReader: MediaMetadataRetriever): Option[Track] =
    if (file.isFile && supportedExtensions.exists(fileExt => file.getName.endsWith(fileExt))) parseTags(file, tagReader)
    else None

  def parseTags(file: File, tagReader: MediaMetadataRetriever): Option[Track] =
    try {
      val path = file.getPath
      val relative = relativize(path)
      tagReader setDataSource path
      def get(metaKey: Int) = tagReader.extractMetadata(metaKey)
      import android.media.MediaMetadataRetriever._
      Some(Track(
        PimpLibrary.encode(relative),
        (Option(get(METADATA_KEY_TITLE)) getOrElse file.getName).trim,
        Option(get(METADATA_KEY_ALBUM)) getOrElse "",
        Option(get(METADATA_KEY_ARTIST)) getOrElse "",
        relative,
        get(METADATA_KEY_DURATION).toLong.milliseconds,
        file.length(),
        Uri.fromFile(file),
        "",
        "",
        None
      ))
    } catch {
      /**
       * May throw:
       *
       * 11-19 19:31:28.730: ERROR/AndroidRuntime(17345): FATAL EXCEPTION: main
       * java.lang.RuntimeException: setDataSource failed: status = 0x80000000
       * at android.media.MediaMetadataRetriever.setDataSource(Native Method)
       * at org.musicpimp.local.LocalLibrary$$anonfun$readTracks$2.apply(LocalLibrary.scala:68)
       */
      case e: Exception =>
        //          warn(s"Unable to read metadata of ${file.getAbsolutePath}", e)
        None
    }

  def relativize(absolutePath: String) = absolutePath.substring(rootPrefixLength)

  def findTrack(track: Track): Option[File] = findPath(track.path)

  def findPath(relativePath: String): Option[File] = {
    val file = new File(rootDirectory, relativePath)
    //    info(s"Searched for relative path $relativePath under $rootDirectory, result: ${file.exists()}")
    if (file.exists()) Some(file) else None
  }

  def contains(track: Track) = findPath(track).isDefined

}

object LocalLibrary extends DiskHelpers with PimpLog {

  /**
   * @param prefs shared preferences
   * @return the amount of data deleted
   */
  def maintainCache(prefs: SharedPreferences): Future[StorageSize] = {
    val cacheSize = prefs.getString(Keys.PREF_CACHE, "5").toInt.gigs
    maintainDirSize(appInternalMusicDir, cacheSize)
  }


  // getExternalFilesDir return null if external storage is not mounted
  val appInternalMusicDir = Option(PimpApp.context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)) getOrElse PimpApp.context.getCacheDir
  val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
  val subsonicMusicDir = new File("/mnt/sdcard/subsonic/music")
}

