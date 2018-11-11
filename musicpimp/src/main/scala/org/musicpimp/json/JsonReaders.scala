package org.musicpimp.json

import org.musicpimp.http.Endpoint
import play.api.libs.json.{JsResult, JsSuccess, JsValue, Reads}

abstract class JsonReaders(endpoint: Endpoint) {
  val username = endpoint.username
  val password = endpoint.password
  val cloudID = endpoint.cloudID

  def constantT[T](c: T) = new Reads[T] {
    override def reads(json: JsValue): JsResult[T] = JsSuccess(c)
  }
}
