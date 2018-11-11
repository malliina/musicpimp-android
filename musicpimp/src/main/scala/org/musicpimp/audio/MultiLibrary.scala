package org.musicpimp.audio

import com.malliina.concurrent.ExecutionContexts.cached
import org.musicpimp.util.PimpLog

import scala.concurrent.Future

trait MultiLibrary extends MediaLibrary with PimpLog {
  def subLibraries: Seq[MediaLibrary]

  def rootFolder: Future[Directory] = mapReduce(subLibraries, _.rootFolder)

  def folder(id: String): Future[Directory] = mapReduce(subLibraries, _.folder(id))

  override def search(term: String, limit: Int): Future[Seq[Track]] = {
    info(s"Searcing!: $term")
    mapReduceSeq[MediaLibrary, Track](subLibraries, _.search(term, limit))
  }

  /** Loads a folder from each sublibrary as specified by parameter f, then adds them all together.
    *
    * The returned future always completes successfully. If the loading of any subdirectory fails,
    * a fallback is used to successfully return the empty directory.
    *
    * @param libraries libraries to load
    * @param f         folder loading function
    * @return a virtual folder, or view, which merges the contents of all sublibrary folders
    */
  protected def mapReduce(libraries: Seq[MediaLibrary], f: MediaLibrary => Future[Directory]): Future[Directory] =
    mapReduceBase[MediaLibrary, Directory](libraries, f, Directory.empty, _ ++ _, _.isEmpty)

  def mapReduceSeq[S, T](libraries: Seq[S], f: S => Future[Seq[T]]) = {
    mapReduceBase[S, Seq[T]](libraries, f, Nil, _ ++ _, _.isEmpty)
  }

  protected def mapReduceBase[S, T](sources: Seq[S],
                                    load: S => Future[T],
                                    empty: T,
                                    merge: (T, T) => T,
                                    isEmpty: T => Boolean): Future[T] =
    Future.sequence(sources.map(lib => load(lib).fallbackTo(Future.successful(empty))))
      .map(_.foldLeft(empty)((acc, dir) => if (isEmpty(dir)) acc else merge(acc, dir)))
}
