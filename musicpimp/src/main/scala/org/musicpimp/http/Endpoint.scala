package org.musicpimp.http

import java.util.UUID

import com.mle.android.http.{IEndpoint, Protocols}
import com.mle.json.SimpleFormat
import org.java_websocket.util.Base64
import org.musicpimp.beam.BeamCode
import play.api.libs.json.Json

case class Endpoint(id: String,
                    name: String,
                    host: String,
                    port: Int,
                    username: String,
                    password: String,
                    endpointType: EndpointTypes.EndpointType = EndpointTypes.MusicPimp,
                    cloudID: Option[String] = None,
                    ssid: Option[String] = None,
                    autoSync: Boolean = true,
                    protocol: Protocols.Protocol = Protocols.Http) extends IEndpoint {

  val supportsSource = endpointType != EndpointTypes.MusicBeamer

  val wsBaseUri = {
    val wsScheme = if (protocol == Protocols.Https) "wss" else "ws"
    baseUri(wsScheme)
  }
  val httpBaseUri = {
    val httpScheme = if (protocol == Protocols.Https) "https" else "http"
    baseUri(httpScheme)
  }

  def authValue =
    if (endpointType != EndpointTypes.Cloud) {
      Endpoint.basicHeader(username, password)
    } else {
      Endpoint.cloudHeader(cloudID.getOrElse(""), username, password)
    }


  private def baseUri(scheme: String) = s"$scheme://$host:$port"

  def httpUri(path: String) = httpBaseUri + path
}

object Endpoint {
  val beamName = "MusicBeamer"
  val beamPassword = "beam"
  val isDev = false
  val (cloudHost, cloudPort, cloudProtocol) =
    if (isDev) ("10.0.2.2", 9000, Protocols.Http)
    else ("cloud.musicpimp.org", 443, Protocols.Https)

  def newID = UUID.randomUUID().toString

  def fromBeamCode(beamCode: BeamCode) =
    Endpoint(newID, beamName, beamCode.host, beamCode.port, beamCode.user, beamPassword, EndpointTypes.MusicBeamer, None, autoSync = false)

  def forCloud(id: Option[String], name: String, cloudID: String, user: String, pass: String) =
    Endpoint(id getOrElse newID, name, cloudHost, cloudPort, user, pass, EndpointTypes.Cloud, Some(cloudID), None, autoSync = false, cloudProtocol)

  implicit object endTypeFormat extends SimpleFormat[EndpointTypes.EndpointType](EndpointTypes.withName)

  implicit object protocolFormat extends SimpleFormat[Protocols.Protocol](Protocols.withName)

  implicit val endFormat = Json.format[Endpoint]

  def header(cloudID: Option[String], user: String, pass: String): String = {
    cloudID.fold(basicHeader(user, pass))(cid => cloudHeader(cid, user, pass))
  }

  def cloudHeader(cloudID: String, user: String, pass: String) = authHeader("Pimp", s"$cloudID:$user:$pass")

  def basicHeader(user: String, pass: String) = authHeader("Basic", s"$user:$pass")

  def authHeader(word: String, unencoded: String) =
    s"$word " + Base64.encodeBytes(unencoded.getBytes("UTF-8"))
}
