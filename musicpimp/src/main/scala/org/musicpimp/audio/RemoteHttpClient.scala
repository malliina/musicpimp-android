package org.musicpimp.audio

import com.mle.android.http.JsonHttpClient
import java.io.Closeable
import org.musicpimp.http.Endpoint
import org.musicpimp.pimp.{PimpWebHttpClient, PimpJsonReaders}
import org.musicpimp.subsonic.SubsonicWebHttpClient

/**
 *
 * @author mle
 */
trait RemoteHttpClient extends Closeable {
  val endpoint: Endpoint
  val client: JsonHttpClient

  override def close(): Unit = client.close()
}

trait PimpHttpClient extends RemoteHttpClient {
  override val client = new PimpWebHttpClient(endpoint)
  val json = new PimpJsonReaders(endpoint)
}

trait SubsonicHttpClient extends RemoteHttpClient {
  override val client = new SubsonicWebHttpClient(endpoint)
}

object SubsonicHttpClient {
  def buildPath(method: String, id: String): String =
    buildPath(method, "id" -> id)

  /**
   * Builds the path part of a Subsonic API URI.
   *
   * @see http://www.subsonic.org/pages/api.jsp
   *
   * @param method subsonic method to use
   * @param params query parameters
   * @return the path part of a URI, e.g. /rest/foo&id=123
   */
  def buildPath(method: String, params: (String, String)*): String = {
    // builds a string like &key1=value1&key2=value2...
    val queryString = params.map {
      case (key, value) => s"$key=$value"
    }.mkString(start = "&", sep = "&", end = "")
    s"/rest/$method.view?v=1.8.0&c=musicpimp-android&f=json" + queryString
  }
}
