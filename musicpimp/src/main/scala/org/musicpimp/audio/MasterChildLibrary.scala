package org.musicpimp.audio

import com.mle.android.http.HttpResponse
import com.mle.concurrent.Futures
import com.mle.concurrent.ExecutionContexts.cached
import com.mle.util.Version
import concurrent.duration.DurationInt
import org.musicpimp.util.Messaging
import scala.concurrent.{TimeoutException, Future}

/**
 *
 * @author mle
 */
class MasterChildLibrary(master: MediaLibrary, child: MediaLibrary) extends MultiLibrary {
  private var isMasterAlive = false

  var subLibraries: Seq[MediaLibrary] = Seq(child)

  pingMasterWithRetry()

  /**
   * Pings the master, retrying once if the first attempt times out.
   *
   * Test thoroughly before removing this, see doc of method `pingMaster`.
   */
  def pingMasterWithRetry() =
    Futures.within(8 seconds)(pingMaster).recoverWith {
      case _: TimeoutException => withErrorHandling(pingMaster)
    }

  /**
   * If this code is in the constructor, sometimes the future never completes
   * even if I assign it to a `val`. I don't know why. If it's in this method,
   * then it always seems to complete so keep it that way, then.
   *
   * @return
   */
  def pingMaster =
    master.ping.map(_ => {
      //      info(s"Got ping response from master, sending reload command")
      isMasterAlive = true
      subLibraries = Seq(master, child)
      // simulates NotifyPropertyChanged
      Messaging.reload()
    })

  def withErrorHandling[T](f: => Future[T]): Future[T] = {
    f.onFailure {
      case e: Exception =>
        Messaging.send("Unable to connect to the music source. Only local tracks will be shown in the library. Check your settings.")
    }
    f
  }

  def ping: Future[Version] = master.ping

  def upload(request: TrackUploadRequest): Future[HttpResponse] = {
    val uploader =
      if (isMasterAlive && (master cacheContains request.track)) master
      else child
    uploader upload request
  }

  def cacheContains(trackId: String): Boolean = subLibraries.exists(_.cacheContains(trackId))

  override protected def mapReduce(libraries: Seq[MediaLibrary], f: MediaLibrary => Future[Directory]): Future[Directory] =
    if (libraries.size > 1) {
      Future.sequence(libraries.map(lib => f(lib).fallbackTo(Future.successful(Directory.empty))))
        .map(dirs => dirs(0) addDistinct dirs(1))
    } else {
      f(libraries.head)
    }
}
