package org.musicpimp.backend

import android.content.Context
import android.util.Base64
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.musicpimp.*

interface Library {
    val name: String
    val client: HttpClient?
    suspend fun folder(id: FolderId): Directory
    suspend fun tracksRecursively(id: FolderId): List<Track>
    suspend fun popular(from: Int, until: Int): PopularTracks
    suspend fun recent(from: Int, until: Int): RecentTracks
}

class EmptyLibrary: Library {
    companion object {
        val instance = EmptyLibrary()
    }

    override val name: String = "Empty"
    override val client: HttpClient? = null
    override suspend fun folder(id: FolderId): Directory = Directory.empty
    override suspend fun tracksRecursively(id: FolderId): List<Track> = emptyList()
    override suspend fun popular(from: Int, until: Int): PopularTracks = PopularTracks(emptyList())
    override suspend fun recent(from: Int, until: Int): RecentTracks = RecentTracks(emptyList())
}

class PimpLibrary(val http: HttpClient, override val name: String): Library {
    companion object {
        val pimpFormat = HeaderValue("application/vnd.musicpimp.v18+json")

        fun build(ctx: Context, authHeader: HeaderValue, name: String): PimpLibrary {
            val http = HttpClient.getInstance(ctx, pimpFormat, authHeader)
            return PimpLibrary(http, name)
        }

        fun authHeader(word: String, unencoded: String): HeaderValue {
            val encoded =
                Base64.encodeToString(unencoded.toByteArray(Charsets.UTF_8), Base64.NO_WRAP).trim()
            return HeaderValue("$word $encoded")
        }

        private val moshi: Moshi = Json.moshi
        val pimpErrorAdapter: JsonAdapter<PimpError> = moshi.adapter(PimpError::class.java)
        val directoryAdapter: JsonAdapter<Directory> = moshi.adapter(Directory::class.java)
        val popularsAdapter: JsonAdapter<PopularTracks> = moshi.adapter(PopularTracks::class.java)
        val recentsAdapter: JsonAdapter<RecentTracks> = moshi.adapter(RecentTracks::class.java)
    }

    override val client: HttpClient?
        get() = http

    override suspend fun folder(id: FolderId): Directory {
        val path = if (id == FolderId.root) "/folders" else "/folders/$id"
        return get(path, directoryAdapter)
    }

    override suspend fun tracksRecursively(id: FolderId): List<Track> {
        val init = folder(id)
        val sub = init.folders.flatMap { f -> tracksRecursively(f.id) }
        return sub + init.tracks
    }

    override suspend fun popular(from: Int, until: Int): PopularTracks {
        return get("/player/popular?from=$from&until=$until", popularsAdapter)
    }

    override suspend fun recent(from: Int, until: Int): RecentTracks {
        return get("/player/recent?from=$from&until=$until", recentsAdapter)
    }

    private suspend fun <T> get(path: String, adapter: JsonAdapter<T>): T {
        return http.getJson(Env.baseUrl.append(path), adapter)
    }
}
