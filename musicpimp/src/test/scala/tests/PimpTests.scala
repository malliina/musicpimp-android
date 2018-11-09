package tests

import org.musicpimp.audio.{Directory, Folder}
import org.musicpimp.http.Endpoint
import org.musicpimp.http.EndpointTypes._
import org.scalatest.FunSuite
import play.api.libs.json.Json._

class PimpTests extends FunSuite {

  val e = Endpoint("testId", "testName", "testHost", 8456, "testUser", "testPass", MusicPimp)

  val eSeq = Seq(e, e.copy(endpointType = Subsonic))

  val dir = Directory(
    Seq(Folder("folder id", "folder path")),
    Seq.empty
  )

  test("can serialize and deserialize endpoint") {
    val serialized = stringify(toJson(e))
    val deserialized = parse(serialized).as[Endpoint]
    assert(e === deserialized)
  }

  test("can serialize seqs") {
    val pair = obj("endpoints" -> toJson(eSeq))
    val serialized = stringify(pair)
    val deserialized = (parse(serialized) \ "endpoints").as[Seq[Endpoint]]
    assert(eSeq === deserialized)
  }

  test("tail") {
    assert(Seq(1).tail === Seq.empty)
  }
}
