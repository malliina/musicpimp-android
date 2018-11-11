package org.musicpimp.andro.util

import com.fasterxml.jackson.core.JsonParseException
import com.mle.util.Utils
import play.api.libs.json.{JsValue, Json}

import scala.util.Try

object Jsons {
  def tryParse(input: String): Try[JsValue] = Try(Json parse input)

  def parse(input: String): Either[JsonParseException, JsValue] =
    Utils.optionally[JsValue, JsonParseException](Json parse input)
}
