package org.musicpimp.audio

import com.mle.util.Utils.executionContext
import org.musicpimp.http.Endpoint
import org.musicpimp.json.JsonStrings._
import org.musicpimp.json.Readers._
import org.musicpimp.pimp._
import org.musicpimp.util.PimpLog
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 *
 * @author mle
 */
abstract class PimpWebSocketPlayer(val endpoint: Endpoint, webSocketResource: String)
  extends Player with PimpHttpClient with PimpLog {

  var socket = newWebSocket

  private def newWebSocket = new PimpWebSocket(endpoint, webSocketResource, onMessage)

  import json._

  def onMessage(json: JsValue): Unit =
    try {
      val event: JsResult[PlayerEvent] = (json \ EVENT).validate[String].flatMap {
        case WELCOME =>
          JsSuccess(Welcomed)
        case STATUS =>
          json.validate[StatusEvent]
        case TIME_UPDATED =>
          (json \ POSITION).validate[Int].map(i => TimeUpdated(i.seconds))
        case PLAYSTATE_CHANGED =>
          (json \ STATE).validate[PlayStates.PlayState] map PlayStateChanged
        case TRACK_CHANGED =>
          // note the subtle hack: if json validation fails we assume the track has changed to "no track"
          val maybeTrack = (json \ TRACK).asOpt[Track]
          JsSuccess(TrackChanged(maybeTrack))
        case PLAYLIST_MODIFIED =>
          (json \ PLAYLIST).validate[Seq[Track]] map PlaylistModified
        case PLAYLIST_INDEX_CHANGED =>
          (json \ PLAYLIST_INDEX).validate[Int].map(jsonValue => {
            val index = if (jsonValue >= 0) Some(jsonValue) else None
            PlaylistIndexChanged(index)
          })
        case VOLUME_CHANGED =>
          (json \ VOLUME).validate[Int] map VolumeChanged
        case MUTE_TOGGLED =>
          (json \ MUTE).validate[Boolean] map MuteToggled
        // Specific to MusicBeamer
        case SUSPENDED_GETTING_DATA =>
          JsSuccess(SuspendedGettingData)
        case SHORT_STATUS =>
          json.validate[ShortStatusEvent]
        case DISCONNECTED =>
          json.validate[Disconnected]
        case js =>
          JsError(s"Unknown event type: $js")
      }
      event.fold(
        jsonErrors => (), // warn(s"Received and ignored invalid JSON. $jsonErrors, received JSON: $json"),
        playerEvent => onWebSocketEvent(playerEvent)
      )
    } catch {
      case e: Exception =>
      //        warn(s"JSON error ${e.getClass.getName}: ${e.getMessage} caused by JSON: $json\n${e.getStackTraceString}")
    }

  def onWebSocketEvent(event: PlayerEvent): Unit =
    fireEvent(event)

  def resume(): Unit = sendSimple(RESUME)

  def pause(): Unit = sendSimple(STOP)

  def mute(muted: Boolean): Unit = sendValued(MUTE, muted)

  /**
   *
   * @param volume [0, 100]
   */
  def volume(volume: Int): Unit = sendValued(VOLUME, volume)

  override def open() = {
    val ret = Try(socket.connect) match {
      case Success(fut) => fut
      case Failure(t) => Future.failed[Unit](t)
    }
    val uri = endpoint.wsBaseUri
    info(s"Connecting to: $uri...")
    ret.map(_ => info(s"Connected to: $uri.")).recover {
      case t: Throwable => warn(s"Connection to: $uri failed.", t)
    }
    ret
  }

  override def close() = {
    client.close()
    closeSocket()
  }

  private def closeSocket(): Unit = Try(socket.close())

  protected def send[T](message: T)(implicit writer: Writes[T]): Unit = {
    socket send message
    //    Try(socket send message).recover {
    //      case _: WebsocketNotConnectedException =>
    //        // tries to reconnect once
    //        Await.ready(reconnect, 5 seconds)
    //        socket send message
    //    }
  }

  //  private def reconnect: Future[Unit] = {
  //    closeSocket()
  //    socket = newWebSocket
  //    socket.connect
  //  }

  protected def sendValued[T](cmd: String, value: T)(implicit writer: Writes[T]): Unit =
    send(ValueCommand(cmd, value))

  protected def sendSimple(cmd: String) = send(SimpleCommand(cmd))

  protected def sendTrack(cmd: String, track: Track) = send(TrackCommand(cmd, track.id))
}
