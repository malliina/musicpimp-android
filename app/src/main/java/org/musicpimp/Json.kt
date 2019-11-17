package org.musicpimp

import com.squareup.moshi.*
import org.musicpimp.endpoints.EndpointAdapter
import java.lang.Exception

class Json {
    companion object {
        val instance = Json()
        val moshi: Moshi get() = instance.moshi

        fun fail(message: String): Nothing = throw JsonDataException(message)
    }

    val moshi: Moshi = Moshi.Builder()
        .add(PrimitiveAdapter())
        .add(EndpointAdapter())
        .add(EnumAdapter())
        .build()
}

class PrimitiveAdapter {
    @FromJson fun cloudId(s: String): CloudId = CloudId(s)
    @ToJson fun writeCloudId(u: CloudId): String = u.value
    @FromJson fun readUsername(s: String): Username = Username(s)
    @ToJson fun writeUsername(u: Username): String = u.value
    @FromJson fun password(s: String): Password = Password(s)
    @ToJson fun writePassword(s: Password): String = s.value
    @FromJson fun folderId(s: String): FolderId = FolderId(s)
    @ToJson fun writeFolderId(s: FolderId): String = s.value
    @FromJson fun trackId(s: String): TrackId = TrackId(s)
    @ToJson fun writeTrackId(s: TrackId): String = s.value
    @FromJson fun artist(s: String): Artist = Artist(s)
    @ToJson fun writeArtist(s: Artist): String = s.value
    @FromJson fun album(s: String): Album = Album(s)
    @ToJson fun writeAlbum(s: Album): String = s.value
    @FromJson fun duration(d: Double): Duration = Duration(d)
    @ToJson fun writeDuration(d: Duration): Double = d.seconds
    @FromJson fun endpointId(s: String): EndpointId = EndpointId(s)
    @ToJson fun writeEndpointId(e: EndpointId): String = e.value
    @FromJson fun url(url: String): FullUrl = FullUrl.build(url) ?: Json.fail("Value '$url' cannot be converted to FullUrl")
    @ToJson fun writeUrl(url: FullUrl): String = url.url
    @FromJson fun nonEmpty(s: String): NonEmptyString? {
        val trimmed = s.trim()
        return if (trimmed.isEmpty()) null else NonEmptyString(trimmed)
    }
    @ToJson fun writeNonEmpty(s: NonEmptyString): String = s.value
}

fun <T> JsonAdapter<T>.read(json: String): T {
    return this.fromJson(json) ?: throw JsonDataException("Moshi returned null when reading '$json'.")
}

fun <T> JsonAdapter<T>.readOpt(json: String): T? {
    return try { this.fromJson(json) } catch (_ : Exception) { null }
}

fun <T> JsonAdapter<T>.readUrl(json: String, url: FullUrl): T {
    return this.fromJson(json) ?: throw JsonDataException("Moshi returned null for response from '$url': '$json'.")
}
