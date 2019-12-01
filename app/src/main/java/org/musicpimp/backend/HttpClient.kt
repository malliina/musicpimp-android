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

class HttpClient(ctx: Context, val authHeader: AuthHeader) {
    companion object {
        const val Authorization = "Authorization"

        fun headers(authHeader: AuthHeader): Map<String, String> {
            val acceptPair = "Accept" to "application/json"
            return mapOf(Authorization to "$authHeader", acceptPair)
        }

        @Volatile
        private var cache: MutableMap<AuthHeader, HttpClient> = mutableMapOf()

        fun getInstance(context: Context, authHeader: AuthHeader): HttpClient =
            cache[authHeader] ?: synchronized(this) {
                cache.getOrPut(authHeader) {
                    HttpClient(context, authHeader)
                }
            }
    }

    private val queue: RequestQueue = Volley.newRequestQueue(ctx.applicationContext)

    // https://jankotlin.wordpress.com/2017/10/16/volley-for-lazy-kotliniers/
    suspend fun getData(url: FullUrl): JSONObject = makeRequest(RequestConf.get(url, authHeader))

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
        return makeRequest(RequestConf(Request.Method.POST, url, authHeader, data))
    }

    private suspend fun send(url: FullUrl, method: Int, data: JSONObject): JSONObject {
        return makeRequest(RequestConf(method, url, authHeader, data))
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

        override fun getHeaders(): Map<String, String> = headers(conf.auth).plus(csrf)
    }
}

data class RequestConf(
    val method: Int,
    val url: FullUrl,
    val auth: AuthHeader,
    val payload: JSONObject?
) {
    companion object {
        fun get(url: FullUrl, auth: AuthHeader): RequestConf =
            RequestConf(Request.Method.GET, url, auth, null)
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
                val pimpError = PimpHttpClient.pimpErrorAdapter.read(str)
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
