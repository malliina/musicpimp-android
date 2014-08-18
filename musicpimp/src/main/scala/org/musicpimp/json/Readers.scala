package org.musicpimp.json

import com.mle.json.JsonFormats
import org.musicpimp.json.JsonStrings._
import org.musicpimp.pimp._
import play.api.libs.json.Json._
import play.api.libs.json._

/**
 *
 * @author mle
 */
object Readers extends JsonFormats {
  implicit def writes[T](implicit genericWrites: Writes[T]) = new Writes[ValueCommand[T]] {
    def writes(o: ValueCommand[T]): JsValue = Json.obj(CMD -> o.cmd, VALUE -> o.value)
  }
}
