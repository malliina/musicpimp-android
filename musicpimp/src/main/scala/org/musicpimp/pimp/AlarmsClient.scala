package org.musicpimp.pimp

import AlarmsClient.{alarmsResource, START, STOP, SAVE, DELETE}
import android.content.Context
import com.mle.android.http.HttpResponse
import com.mle.util.Version
import org.musicpimp.http.Endpoint
import org.musicpimp.pimp.Alarms._
import play.api.libs.json.Writes
import scala.concurrent.Future

/**
 * @author KING MICHAEL
 */
class AlarmsClient(endpoint: Endpoint) extends PimpWebHttpClient(endpoint) {
  val player = new PimpServerPlayer(endpoint)

  def ping: Future[Version] = getJson[Version](PimpLibrary.pingAuthResource)

  def alarms: Future[Seq[Alarm]] = getJson[Seq[Alarm]](alarmsResource)

  def save(ctx: Context, alarm: IdAlarm): Future[HttpResponse] = postAlarmBody(ctx, AlarmCmd(SAVE, alarm))

  def start(ctx: Context, id: String): Future[HttpResponse] = postAlarmBody(ctx, ShortCmd(START, id))

  def remove(ctx: Context, id: String): Future[HttpResponse] = postAlarmBody(ctx, ShortCmd(DELETE, id))

  def stop(ctx: Context): Future[HttpResponse] = postAlarmBody(ctx, ShortestCmd(STOP))

  protected def postAlarmBody[T](ctx: Context, body: T)(implicit writer: Writes[T]) = postBody(ctx, alarmsResource, body)

  def connect() = player.open()

  override def close(): Unit = {
    player.close()
    super.close()
  }
}

object AlarmsClient {
  val alarmsResource = "/alarms"
  val START = "start"
  val STOP = "stop"
  val SAVE = "save"
  val DELETE = "delete"
}
