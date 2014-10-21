package org.musicpimp.andro.messaging

import JsonMessagingUtils._
import android.content.Context
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.http.Endpoint
import org.musicpimp.pimp.{AlarmsClient, PimpWebHttpClient}
import play.api.libs.json.Json
import scala.concurrent.Future

/**
 *
 * @author mle
 */
class JsonMessagingUtils(endpoint: Endpoint, registerCommand: String, unregisterCommand: String) {
  val client = new PimpWebHttpClient(endpoint)

  def registerId(ctx: Context, id: String): Future[String] = {
    postCmd(ctx, registerCommand, id)
  }

  def unregisterId(ctx: Context, id: String): Future[String] = {
    postCmd(ctx, unregisterCommand, id)
  }

  private def postCmd(ctx: Context, cmd: String, id: String): Future[String] =
    client.post(ctx, AlarmsClient.alarmsResource, Json.toJson(Map(CMD -> cmd, ID -> id, TAG -> endpoint.id))).map(_ => id)
}

class GcmUtils(endpoint: Endpoint) extends JsonMessagingUtils(endpoint, GCM_ADD, GCM_REMOVE)

class AdmUtils(endpoint: Endpoint) extends JsonMessagingUtils(endpoint, ADM_ADD, ADM_REMOVE)

object JsonMessagingUtils {
  val ID = "id"
  val TAG = "tag"
  val CMD = "cmd"
  val GCM_ADD = "gcm_add"
  val GCM_REMOVE = "gcm_remove"
  val ADM_ADD = "adm_add"
  val ADM_REMOVE = "adm_remove"
  val STOP = "stop"
}
