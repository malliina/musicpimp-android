package org.musicpimp.audio

import com.mle.android.exceptions.ExplainedException
import com.mle.android.http.HttpResponse
import com.mle.util.Utils.executionContext
import com.mle.util.Version
import org.musicpimp.PimpApp
import scala.concurrent.Future

/**
 *
 * @author mle
 */
class MasterLibrary(libraries: Seq[MediaLibrary]) extends MultiLibrary {
  var subLibraries: List[MediaLibrary] = List.empty
  val pairs = libraries zip libraries.map(_.ping)
  // takes libraries into use following a successful response to ping
  pairs.foreach {
    case (lib, pingSuccess) => pingSuccess.foreach(_ => subLibraries = lib :: subLibraries)
  }

  val ping: Future[Version] = Future.successful(PimpApp.version)

  def cacheContains(trackId: String): Boolean = findLibrary(trackId).isDefined

  private def findLibrary(trackId: String) = subLibraries.find(_.cacheContains(trackId))

  def upload(request: TrackUploadRequest): Future[HttpResponse] =
    findLibrary(request.track).map(_.upload(request))
      .getOrElse(throw new ExplainedException(s"Unable to find track with ID: ${request.track} in any active music library."))
}