package tests

import org.musicpimp.util.{BasicMessage, Messaging, UIMessage}
import org.scalatest.FunSuite

class UtilTests extends FunSuite {
  test("an added message handler handles messages, a removed one does not") {
    var latestMessage = "nothing"
    val handler: PartialFunction[UIMessage, Unit] = {
      case BasicMessage(msg) => latestMessage = msg
      case _ => ()
    }
    val message = "Handle this"
    val unhandledMessage = "You should not handle this"
    Messaging.addHandler(handler)
    Messaging.send(message)
    assert(latestMessage === message)
    Messaging.removeHandler(handler)
    Messaging.send(unhandledMessage)
    assert(latestMessage === message)
  }
  
  test("views") {
    val in = Seq(1, 2, 3, 4, 5)
    val result = in.view.map(incr).find(_.contains(102)).flatten
    assert(result === Some(102))
  }

  def incr(i: Int): Option[Int] = {
//    println(s"Maybe incrementing $i")
    val tmp = i % 2
//    println(s"tmp: $tmp")
    if (i % 2 == 0) {
//      println("some yo")
      Some(i + 100)
    } else {
      None
    }
  }
}
