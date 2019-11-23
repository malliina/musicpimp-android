package org.musicpimp.backend

import com.neovisionaries.ws.client.*
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.suspendCancellableCoroutine
import org.musicpimp.FullUrl
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class WebSocketClient(val url: FullUrl, headers: Map<String, String>) {
    companion object {
        val baseUrl = Env.baseUrl
    }

    abstract fun onMessage(message: String)

    private val sf: WebSocketFactory = WebSocketFactory()
    // var because it's recreated on reconnects
    private var socket = sf.createSocket(url.url, 10000)
    private val listener = object : WebSocketAdapter() {
        override fun onTextMessage(websocket: WebSocket?, text: String?) {
            try {
                text?.let { onMessage(it) }
            } catch (e: Exception) {
                Timber.e(e, "JSON error.")
            }
        }

        override fun onDisconnected(
            websocket: WebSocket?,
            serverCloseFrame: WebSocketFrame?,
            clientCloseFrame: WebSocketFrame?,
            closedByServer: Boolean
        ) {
            Timber.i("Disconnected from '$url'.")
        }
    }

    init {
        socket.addListener(listener)
        headers.forEach { (k, v) -> socket.addHeader(k, v) }
    }

    suspend fun connect(): WebSocket? =
        suspendCancellableCoroutine { cont ->
            Timber.i("Connecting to '$url'...")
            val connectCallback = object : WebSocketAdapter() {
                override fun onConnected(
                    websocket: WebSocket?,
                    headers: MutableMap<String, MutableList<String>>?
                ) {
                    Timber.i("Connected to '$url'.")
                    socket.removeListener(this)
                    cont.resume(websocket)
                }

                override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {
                    Timber.w("Unable to connect to '$url'.")
                    val e = exception ?: Exception("Unable to connect to '$url'.")
                    socket.removeListener(this)
                    cont.resumeWithException(e)
                }
            }
            socket.addListener(connectCallback)
            socket.connectAsynchronously()
        }

    fun <T> send(message: T, adapter: JsonAdapter<T>) {
        // Might throw
        if (socket.isOpen) {
            val json = adapter.toJson(message)
            Timber.d("Sending $json...")
            socket.sendText(json)
        }
    }

    fun disconnect() {
        socket.removeListener(listener)
        socket.disconnect()
    }
}