package org.musicpimp.backend

import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.musicpimp.*
import timber.log.Timber
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpClient(ctx: Context, private val headers: Map<String, String>) {
    companion object {
        const val Authorization = "Authorization"

        fun pimpHeaders(accept: HeaderValue, auth: HeaderValue): Map<String, String> {
            val acceptPair = "Accept" to "$accept"
            return mapOf(Authorization to "$auth", acceptPair)
        }

        fun headered(ctx: Context, accept: HeaderValue, authHeader: HeaderValue) {
            HttpClient(ctx, pimpHeaders(accept, authHeader))
        }

        @Volatile
        private var cache: MutableMap<HeaderValue, HttpClient> = mutableMapOf()

        fun getInstance(context: Context, accept: HeaderValue, auth: HeaderValue): HttpClient =
            cache[auth] ?: synchronized(this) {
                cache.getOrPut(auth) {
                    HttpClient(context, pimpHeaders(accept, auth))
                }
            }
    }

    private val queue: RequestQueue = Volley.newRequestQueue(ctx.applicationContext)

    // https://jankotlin.wordpress.com/2017/10/16/volley-for-lazy-kotliniers/
    private suspend fun getData(url: FullUrl): JSONObject = makeRequest(RequestConf.get(url, headers))

    suspend fun <T> getJson(url: FullUrl, adapter: JsonAdapter<T>): T {
        val json = getData(url)
        return adapter.readUrl(json.toString(), url)
    }

    suspend fun <T, U> put(
        url: FullUrl,
        payload: T,
        request: JsonAdapter<T>,
        response: JsonAdapter<U>
    ): U {
        val json = send(url, Request.Method.PUT, JSONObject(request.toJson(payload)))
        return response.read(json.toString())
    }

    suspend fun post(url: FullUrl, data: JSONObject): JSONObject {
        return makeRequest(RequestConf(Request.Method.POST, url, headers, data))
    }

    private suspend fun send(url: FullUrl, method: Int, data: JSONObject): JSONObject {
        return makeRequest(RequestConf(method, url, headers, data))
    }

    private suspend fun makeRequest(conf: RequestConf): JSONObject =
        suspendCancellableCoroutine { cont ->
            RequestWithHeaders(conf, cont).also {
                queue.add(it)
            }
        }

    class RequestWithHeaders(
        private val conf: RequestConf,
        cont: CancellableContinuation<JSONObject>
    ) : JsonObjectRequest(conf.method, conf.url.url, conf.payload,
        Response.Listener { cont.resume(it) },
        Response.ErrorListener { error ->
            val exception = ResponseException(error, conf)
            try {
                val errors = exception.errors()
                Timber.e("Request failed with errors $errors.")
                // This try-catch is only for error logging purposes; the error must be handled by the caller later
            } catch (e: Exception) {
            }
            cont.resumeWithException(exception)
        }) {
        private val httpMethod = conf.method
        private val csrf =
            if (httpMethod == Method.POST || httpMethod == Method.PUT || httpMethod == Method.DELETE)
                mapOf("Csrf-Token" to "nocheck", "Content-Type" to "application/json")
            else
                emptyMap()

        override fun getHeaders(): Map<String, String> = conf.headers.plus(csrf)
    }
}

data class RequestConf(
    val method: Int,
    val url: FullUrl,
    val headers: Map<String, String>,
    val payload: JSONObject?
) {
    companion object {
        fun get(url: FullUrl, headers: Map<String, String>): RequestConf =
            RequestConf(Request.Method.GET, url, headers, null)
    }
}

data class ResponseException(val error: VolleyError, val req: RequestConf) :
    Exception("Invalid response", error.cause) {
    private val url = req.url
    private val response: NetworkResponse? = error.networkResponse

    fun errors(): Errors {
        return if (response != null) {
            val response = response
            try {
                val charset =
                    Charset.forName(HttpHeaderParser.parseCharset(response.headers, "UTF-8"))
                val str = String(response.data, charset)
                val pimpError = PimpLibrary.pimpErrorAdapter.read(str)
                Errors.single("server", pimpError.reason)
            } catch (e: Exception) {
                val msg = "Unable to parse response from '$url'."
                Timber.e(e, msg)
                Errors.input(msg)
            }
        } else {
            Errors.single("network", "Network error from '$url'.")
        }
    }
}
