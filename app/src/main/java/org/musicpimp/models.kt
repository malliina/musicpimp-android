package org.musicpimp

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.musicpimp.backend.HttpClient
import org.musicpimp.backend.PimpHttpClient
import java.util.*
import java.util.regex.Pattern

interface Primitive : CharSequence {
    val value: String
    override val length: Int get() = value.length
    override fun get(index: Int): Char = value.get(index)
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        value.subSequence(startIndex, endIndex)
}

interface MusicItem<T> {
    val id: T
    val title: String
}

@Parcelize
data class FolderId(override val value: String) : Primitive, Parcelable {
    companion object {
        val root = FolderId("")
    }

    override fun toString(): String = value
}

data class TrackId(override val value: String) : Primitive {
    override fun toString(): String = value
}

data class TrackTitle(override val value: String) : Primitive {
    override fun toString(): String = value
}

data class Artist(override val value: String) : Primitive {
    override fun toString(): String = value
}

data class Album(override val value: String) : Primitive {
    override fun toString(): String = value
}

data class CloudId(override val value: String) : Primitive {
    override fun toString(): String = value
}

data class Username(override val value: String) : Primitive {
    override fun toString(): String = value
}

data class Password(override val value: String) : Primitive {
    override fun toString(): String = value
}

data class AuthHeader(override val value: String) : Primitive {
    override fun toString(): String = value
}

data class NonEmptyString(override val value: String) : Primitive {
    override fun toString(): String = value
}

@Parcelize
data class EndpointId(override val value: String) : Primitive, Parcelable {
    override fun toString(): String = value

    companion object {
        fun random(): EndpointId = EndpointId(UUID.randomUUID().toString().take(5))
    }
}

@JsonClass(generateAdapter = true)
data class Folder(override val id: FolderId, override val title: String, val path: String) :
    MusicItem<FolderId>

@JsonClass(generateAdapter = true)
data class Track(
    override val id: TrackId,
    override val title: String,
    val album: Album,
    val artist: Artist,
    val path: String,
    val duration: Duration,
    val size: Long,
    val url: FullUrl
) : MusicItem<TrackId>

@JsonClass(generateAdapter = true)
data class Directory(val folder: Folder, val folders: List<Folder>, val tracks: List<Track>) {
    val size = folders.size + tracks.size
    val isEmpty = size == 0

    companion object {
        val empty = Directory(Folder(FolderId.root, "", ""), emptyList(), emptyList())
    }
}

data class Duration(val seconds: Double) {
    companion object {
        private fun formatSeconds(seconds: Long): String {
            val s = seconds % 60
            val m = (seconds / 60) % 60
            val h = (seconds / (60 * 60)) % 24
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }

        fun seconds(seconds: Long): Duration = Duration(seconds.toDouble())
    }

    override fun toString() = formatted()

    fun formatted(): String = formatSeconds(seconds.toLong())
}

data class FullUrl(val proto: String, val hostAndPort: String, val uri: String) {
    private val host = hostAndPort.takeWhile { c -> c != ':' }
    private val protoAndHost = "$proto://$hostAndPort"
    val url = "$protoAndHost$uri"

    fun append(more: String) = copy(uri = this.uri + more)

    override fun toString(): String = url

    companion object {
        private val pattern = Pattern.compile("(.+)://([^/]+)(/?.*)")

        fun https(domain: String, uri: String): FullUrl = FullUrl("https", dropHttps(domain), uri)
        fun http(domain: String, uri: String): FullUrl = FullUrl("http", dropHttps(domain), uri)
        fun host(domain: String): FullUrl = FullUrl("https", dropHttps(domain), "")
        fun ws(domain: String, uri: String): FullUrl = FullUrl("ws", domain, uri)
        fun wss(domain: String, uri: String): FullUrl = FullUrl("wss", domain, uri)

        fun parse(input: String): FullUrl {
            return build(input)
                ?: throw JSONException("Value $input cannot be converted to FullUrl")
        }

        fun build(input: String): FullUrl? {
            val m = pattern.matcher(input)
            return if (m.find() && m.groupCount() == 3) {
                m.group(1)?.let { g1 ->
                    m.group(2)?.let { g2 ->
                        m.group(3)?.let { g3 ->
                            FullUrl(g1, g2, g3)
                        }
                    }
                }
            } else {
                null
            }
        }

        private fun dropHttps(domain: String): String {
            val prefix = "https://"
            return if (domain.startsWith(prefix)) domain.drop(prefix.length) else domain
        }
    }
}

@JsonClass(generateAdapter = true)
data class PimpError(val reason: String)

@JsonClass(generateAdapter = true)
data class SingleError(val key: String, val message: String) {
    companion object {
        fun backend(message: String) = SingleError("backend", message)
    }
}

@JsonClass(generateAdapter = true)
data class Errors(val errors: List<SingleError>) {
    companion object {
        fun input(message: String) = single("input", message)
        fun single(key: String, message: String): Errors = Errors(listOf(SingleError(key, message)))
    }
}

interface Credential {
    val authHeader: AuthHeader
}

@JsonClass(generateAdapter = true)
data class CloudCredential(val server: CloudId, val username: Username, val password: Password) :
    Credential {
    override val authHeader: AuthHeader
        get() = PimpHttpClient.authHeader("Pimp", "$server:$username:$password")
}

@JsonClass(generateAdapter = true)
data class DirectCredential(val username: Username, val password: Password) : Credential {
    override val authHeader: AuthHeader
        get() = PimpHttpClient.authHeader("Basic", "$username:$password")
}
