package tests

import com.mle.ws.JsonWebSocketClient
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class WsTests extends FunSuite {
  test("can connect with ws") {
    val client = new JsonWebSocketClient("ws://192.168.0.12:9000/ws/playback", None)
    val fut = client.connect()
    Await.result(fut, 5.seconds)
  }
}
