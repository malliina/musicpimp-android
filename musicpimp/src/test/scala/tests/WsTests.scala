package tests

import com.malliina.http.FullUrl
import com.malliina.ws.JsonWebSocketClient
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class WsTests extends FunSuite {
  ignore("can connect with ws") {
    val client = new JsonWebSocketClient(FullUrl.ws("192.168.0.12:9000", "/ws/playback"), None, Map.empty)
    val fut = client.connect()
    Await.result(fut, 5.seconds)
  }
}
