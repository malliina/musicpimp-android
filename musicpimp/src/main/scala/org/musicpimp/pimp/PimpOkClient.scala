package org.musicpimp.pimp

import java.io.File

import com.malliina.android.exceptions.AndroidException
import com.malliina.android.http.HttpConstants.{ACCEPT, AUTHORIZATION, JSON}
import com.malliina.android.http.Protocols
import com.malliina.http.OkClient.OkResponse
import com.malliina.http.{FullUrl, OkClient, ResponseError}
import okio.Okio
import org.musicpimp.http.Endpoint
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

object PimpOkClient {
  val ok = OkClient.default

  def pimp(endpoint: Endpoint) = {
    val headers = Map(
      AUTHORIZATION -> endpoint.authValue,
      ACCEPT -> PimpConstants.JSONv18
    )
    apply(endpoint, headers)
  }

  // TODO fail 200 OK requests also if payload is shady
  def subsonic(endpoint: Endpoint) = apply(endpoint, Map(ACCEPT -> JSON))

  def apply(endpoint: Endpoint, headers: Map[String, String]) =
    new PimpOkClient(endpoint, ok, headers)(ok.exec)
}

class PimpOkClient(val endpoint: Endpoint, val client: OkClient, headers: Map[String, String])(implicit ec: ExecutionContext) {
  val baseUrl = FullUrl(scheme, s"${endpoint.host}:${endpoint.port}", "")

  def getEmpty(uri: String): Future[Unit] = client.get(toUrl(uri), headers).map(_ => ())

  def getUri[T: Reads](uri: String): Future[T] = {
    val url = toUrl(uri)
    get[T](url).flatMap(e => fold(url, e))
  }

  private def fold[T](url: FullUrl, e: Either[ResponseError, T]): Future[T] =
    e.fold(err => Future.failed(new AndroidException(s"HTTP error $err")), Future.successful)

  def get[T: Reads](url: FullUrl): OkResponse[T] = client.getAs[T](url, headers)

  def getFile(uri: String, file: File): Future[File] = client.get(toUrl(uri), headers).map { r =>
    val sink = Okio.buffer(Okio.sink(file))
    sink.writeAll(r.inner.body().source())
    sink.close()
    r.inner.close()
    file
  }

  def toUrl(uri: String): FullUrl = baseUrl / uri

  private def scheme = if (endpoint.protocol == Protocols.Https) "https" else "http"

  def close(): Unit = client.close()
}
