package org.musicpimp.audio

import com.malliina.concurrent.ExecutionContexts.cached
import org.musicpimp.http.Endpoint
import play.api.libs.json.Reads
import scala.collection.mutable
import scala.concurrent.Future

abstract class RemoteMediaLibrary(val endpoint: Endpoint) extends MediaLibrary with RemoteHttpClient {
  // cache keyed by folder ID
  protected val cache = mutable.Map.empty[String, Directory]
  implicit val reader: Reads[Directory]

  override def invalidateCache(): Unit = cache.clear()

  def get[T](resource: String)(implicit fjs: Reads[T]): Future[T] = client.getJson[T](resource)

  def getWithCache(id: String)(implicit fjs: Reads[Directory]): Future[Directory] = {
    // updates the cache only if the request returns successfully
    cache.get(id).fold({
      val futureDirectory = get[Directory](resource(id))(fjs)
      futureDirectory.foreach(dir => cache.update(id, dir))
      futureDirectory
    })(Future.successful)
    // the problem with the following line is that also failed requests would be stored in the cache
    //    cache.getOrElseUpdate(id, get(id))
  }

  def rootFolder: Future[Directory] =
    getWithCache(rootFolderId)

  protected def rootFolderId = ""

  /**
   * @param folderId the folder ID
   * @return the path component of a URI that points to the folder with the given id
   */
  def resource(folderId: String): String

  def cacheContains(trackId: String): Boolean =
    cache.values.flatMap(_.tracks).exists(_.id == trackId)
}

