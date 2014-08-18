package org.musicpimp.http

import com.mle.android.http.{IEndpoint, Protocols}
import com.mle.json.SimpleFormat
import java.util.UUID
import org.musicpimp.beam.BeamCode
import play.api.libs.json.Json

/**
 *
 * @author mle
 */
case class Endpoint(id: String,
                    name: String,
                    host: String,
                    port: Int,
                    username: String,
                    password: String,
                    endpointType: EndpointTypes.EndpointType = EndpointTypes.MusicPimp,
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

  private def baseUri(scheme: String) = s"$scheme://$host:$port"

  def httpUri(path: String) = httpBaseUri + path
}

object Endpoint {
  val beamName = "MusicBeamer"
  val beamPassword = "beam"

  def newID = UUID.randomUUID().toString

  def fromBeamCode(beamCode: BeamCode) =
    new Endpoint(newID, beamName, beamCode.host, beamCode.port, beamCode.user, beamPassword, EndpointTypes.MusicBeamer)

  implicit object endTypeFormat extends SimpleFormat[EndpointTypes.EndpointType](EndpointTypes.withName)

  implicit object protocolFormat extends SimpleFormat[Protocols.Protocol](Protocols.withName)

  implicit val endFormat = Json.format[Endpoint]
}
