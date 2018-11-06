package tests

import org.scalatest.FunSuite

class RegexTests extends FunSuite {
  test("regex") {
    val folderId = id("blaa/getMusicDirectory&id=50&blaa")
    assert(folderId === 50)
  }

  def id(res: String) = {
    val pattern = """.*getMusicDirectory&id=(\d+).*""".r
    val pattern(id) = "blaa/getMusicDirectory&id=50&blaa"
    id.toInt
  }
}
