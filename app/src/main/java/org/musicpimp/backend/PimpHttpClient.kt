package org.musicpimp.backend

import android.content.Context
import android.util.Base64
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.musicpimp.*

class PimpHttpClient(val http: HttpClient, val name: String) {
    companion object {
        const val pimpFormat = "application/vnd.musicpimp.v18+json"

        fun build(ctx: Context, authHeader: AuthHeader, name: String): PimpHttpClient {
            val http = HttpClient.getInstance(ctx, authHeader)
            return PimpHttpClient(http, name)
        }

        fun authHeader(word: String, unencoded: String): AuthHeader {
            val encoded =
                Base64.encodeToString(unencoded.toByteArray(Charsets.UTF_8), Base64.NO_WRAP).trim()
            return AuthHeader("$word $encoded")
        }

        private val moshi: Moshi = Json.moshi
        val pimpErrorAdapter: JsonAdapter<PimpError> = moshi.adapter(PimpError::class.java)
        val directoryAdapter: JsonAdapter<Directory> = moshi.adapter(Directory::class.java)
        val popularsAdapter: JsonAdapter<PopularTracks> = moshi.adapter(PopularTracks::class.java)
        val recentsAdapter: JsonAdapter<RecentTracks> = moshi.adapter(RecentTracks::class.java)
    }

    suspend fun folder(id: FolderId): Directory {
        val path = if (id == FolderId.root) "/folders" else "/folders/$id"
        return get(path, directoryAdapter)
    }

    suspend fun popular(from: Int, until: Int): PopularTracks {
        return get("/player/popular?from=$from&until=$until", popularsAdapter)
    }

    suspend fun recent(from: Int, until: Int): RecentTracks {
        return get("/player/recent?from=$from&until=$until", recentsAdapter)
    }

    private suspend fun <T> get(path: String, adapter: JsonAdapter<T>): T {
        return http.getJson(Env.baseUrl.append(path), adapter)
    }
}
