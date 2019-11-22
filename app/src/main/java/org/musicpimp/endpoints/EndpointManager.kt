package org.musicpimp.endpoints

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.*
import org.musicpimp.*
import org.musicpimp.Json
import timber.log.Timber

class EndpointAdapter {
    @ToJson
    fun toJson(e: Endpoint): String {
        return when (e) {
            is DirectEndpoint -> EndpointManager.directAdapter.toJson(e)
            is CloudEndpoint -> EndpointManager.cloudAdapter.toJson(e)
            else -> throw JsonDataException("Cannot serialize endpoint $e.")
        }
    }

    @FromJson
    fun fromJson(str: String): Endpoint? {
        Timber.i("Reading '$str'...")
        return EndpointManager.cloudAdapter.readOpt(str) ?: EndpointManager.directAdapter.readOpt(
            str
        )
    }
}

interface Endpoint {
    val id: EndpointId
    val name: NonEmptyString
}

interface EndpointInput<T : Endpoint> {
    fun withId(id: EndpointId): T
}

data class DirectEndpointInput(
    val name: NonEmptyString,
    val address: NonEmptyString,
    val port: Int,
    val creds: DirectCredential
) : EndpointInput<DirectEndpoint> {
    override fun withId(id: EndpointId): DirectEndpoint =
        DirectEndpoint(id, name, address, port, creds)
}

@JsonClass(generateAdapter = true)
data class DirectEndpoint(
    override val id: EndpointId,
    override val name: NonEmptyString,
    val address: NonEmptyString,
    val port: Int,
    val creds: DirectCredential
) : Endpoint

data class CloudEndpointInput(
    val name: NonEmptyString,
    val creds: CloudCredential
) : EndpointInput<CloudEndpoint> {
    override fun withId(id: EndpointId): CloudEndpoint = CloudEndpoint(id, name, creds)
}

@JsonClass(generateAdapter = true)
data class CloudEndpoint(
    override val id: EndpointId,
    override val name: NonEmptyString,
    val creds: CloudCredential
) : Endpoint

@JsonClass(generateAdapter = true)
data class Endpoints(val endpoints: List<Endpoint>) {
    fun plus(endpoint: Endpoint) = Endpoints(endpoints.plus(endpoint))
    fun remove(id: EndpointId) = Endpoints(endpoints.filterNot { e -> e.id == id })
}

@JsonClass(generateAdapter = true)
data class ActiveEndpoint(val id: EndpointId)

class EndpointManager(private val prefs: SharedPreferences) {
    companion object {
        const val activeKey = "org.musicpimp.prefs.endpoints.active"
        const val endpointsKey = "org.musicpimp.prefs.endpoints.list"

        val activeAdapter: JsonAdapter<ActiveEndpoint> =
            Json.moshi.adapter(ActiveEndpoint::class.java)
        val directAdapter: JsonAdapter<DirectEndpoint> =
            Json.moshi.adapter(DirectEndpoint::class.java)
        val cloudAdapter: JsonAdapter<CloudEndpoint> = Json.moshi.adapter(CloudEndpoint::class.java)
        val endpointsAdapter: JsonAdapter<Endpoints> = Json.moshi.adapter(Endpoints::class.java)

        fun load(app: Application): EndpointManager {
            return EndpointManager(
                app.getSharedPreferences(
                    "org.musicpimp.prefs",
                    Context.MODE_PRIVATE
                )
            )
        }
    }

    fun active(): CloudEndpoint? {
        return loadOpt(activeKey, activeAdapter)?.let { id ->
            fetch().endpoints.find { e -> e.id == id.id }
                ?.let { e -> if (e is CloudEndpoint) e else null }
        }
    }

    fun saveActive(id: EndpointId) {
        save(ActiveEndpoint(id), activeAdapter, activeKey)
    }

    fun save(endpoint: Endpoint): Endpoints {
        val newValue =
            Endpoints(fetch().endpoints.filterNot { e -> e.id == endpoint.id }.plus(endpoint))
        saveAll(newValue)
        return newValue
    }

    fun saveAll(endpoints: Endpoints) {
        save(endpoints, endpointsAdapter, endpointsKey)
    }

    fun remove(endpointId: EndpointId) {
        saveAll(fetch().remove(endpointId))
    }

    fun fetch(): Endpoints = load(endpointsKey, endpointsAdapter, Endpoints(emptyList()))

    fun <T> load(key: String, adapter: JsonAdapter<T>, default: T): T {
        return loadOpt(key, adapter) ?: default
    }

    fun <T> loadOpt(key: String, adapter: JsonAdapter<T>): T? {
        val str = prefs.getString(key, null)
        return str?.let { adapter.fromJson(it) }
    }

    fun <T> save(item: T, adapter: JsonAdapter<T>, to: String) {
        prefs.edit {
            val json = adapter.toJson(item)
            putString(to, json)
            Timber.i("Saved $json.")
        }
    }
}
