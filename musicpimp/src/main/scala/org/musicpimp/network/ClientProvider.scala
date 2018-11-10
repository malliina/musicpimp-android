package org.musicpimp.network

/** TODO embrace FP
  */
class ClientProvider(clientCount: Int) {
  // each AsyncHttpClient instance only does 10 parallel connections, so we use multiple
  val clients = Seq.fill(clientCount)(new PingHttpClient)
  var id = 0

  def nextClient: PingHttpClient = {
    id += 1
    clients(id % clientCount)
  }

  def close(): Unit = clients.foreach(_.close())
}