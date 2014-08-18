package tests

import org.scalatest.FunSuite
import scala.concurrent.Await
import concurrent.duration._
import com.mle.android.websockets.JsonWebSocketClient

/**
 *
 * @author mle
 */
class WsTests extends FunSuite {
  test("can connect with ws") {
    val client = new JsonWebSocketClient("ws://192.168.0.12:9000/ws/playback", "admin", "test")
    val fut = client.connect
    Await.result(fut, 5 seconds)
  }
}
