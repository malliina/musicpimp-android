package tests

import org.musicpimp.json.JsonStrings._
import org.scalatest.FunSuite
import play.api.libs.json.{JsValue, Json, Writes}

class JsonTests extends FunSuite {
  test("json") {
    case class GenericCommand[T](cmd: String, value: T)
    implicit def writes[T: Writes] = new Writes[GenericCommand[T]] {
      def writes(o: GenericCommand[T]): JsValue = Json.obj(CMD -> o.cmd, VALUE -> o.value)
    }

    val testAhum = GenericCommand("abba", 666)
    val serialized = Json.stringify(Json.toJson(testAhum))
    assert(serialized.contains("abba"))
  }
}
