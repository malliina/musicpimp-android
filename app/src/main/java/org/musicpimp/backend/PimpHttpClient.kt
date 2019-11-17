package org.musicpimp.backend

import android.content.Context
import android.util.Base64
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.musicpimp.*

class PimpHttpClient(val http: HttpClient) {
    companion object {
        const val pimpFormat = "application/vnd.musicpimp.v18+json"

        fun build(ctx: Context, authHeader: AuthHeader): PimpHttpClient {
            val http = HttpClient.getInstance(ctx, authHeader)
            return PimpHttpClient(http)
        }

        fun authHeader(word: String, unencoded: String): AuthHeader {
            val encoded =
                Base64.encodeToString(unencoded.toByteArray(Charsets.UTF_8), Base64.NO_WRAP).trim()
            return AuthHeader("$word $encoded")
        }

        private val moshi: Moshi = Json.moshi
        //        val errorsAdapter: JsonAdapter<Errors> = moshi.adapter(Errors::class.java)
        val pimpErrorAdapter: JsonAdapter<PimpError> = moshi.adapter(PimpError::class.java)
        val directoryAdapter: JsonAdapter<Directory> = moshi.adapter(Directory::class.java)
    }

    suspend fun folder(id: FolderId): Directory {
        val path = if (id == FolderId.root) "/folders" else "/folders/$id"
        return http.getJson(Env.baseUrl.append(path), directoryAdapter)
    }
}
