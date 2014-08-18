package org.musicpimp.audio

import com.mle.util.Utils.executionContext
import scala.concurrent.Future

/**
 *
 * @author mle
 */
trait MultiLibrary extends MediaLibrary {
  def subLibraries: Seq[MediaLibrary]

  def rootFolder: Future[Directory] = mapReduce(subLibraries, _.rootFolder)

  def folder(id: String): Future[Directory] = mapReduce(subLibraries, _.folder(id))

  /**
   * Loads a folder from each sublibrary as specified by parameter f, then adds them all together.
   *
   * The returned future always completes successfully. If the loading of any subdirectory fails,
   * a fallback is used to successfully return the empty directory.
   *
   * @param libraries libraries to load
   * @param f folder loading function
   * @return a virtual folder, or view, which merges the contents of all sublibrary folders
   */
  protected def mapReduce(libraries: Seq[MediaLibrary], f: MediaLibrary => Future[Directory]): Future[Directory] =
    Future.sequence(libraries.map(lib => f(lib).fallbackTo(Future.successful(Directory.empty))))
      .map(_.foldLeft(Directory.empty)((acc, dir) => if (dir.isEmpty) acc else acc ++ dir))
}
