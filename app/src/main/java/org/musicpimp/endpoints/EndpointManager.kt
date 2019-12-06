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
//            is DirectEndpoint -> EndpointManager.directAdapter.toJson(e)
            is CloudEndpoint -> EndpointManager.cloudAdapter.toJson(e)
            is LocalEndpoint -> EndpointManager.localAdapter.toJson(e)
            else -> throw JsonDataException("Cannot serialize endpoint $e.")
        }
    }

    @FromJson
    fun fromJson(str: String): Endpoint? {
        Timber.i("Reading '$str'...")
        return EndpointManager.cloudAdapter.readOpt(str)
            ?: EndpointManager.localAdapter.readOpt(str)
    }
}

class EndpointManager(private val prefs: SharedPreferences) {
    companion object {
        const val activePlayerKey = "org.musicpimp.prefs.endpoints.active.player"
        const val activeSourceKey = "org.musicpimp.prefs.endpoints.active.source"
        const val endpointsKey = "org.musicpimp.prefs.endpoints.list"

        val activeAdapter: JsonAdapter<ActiveEndpoint> =
            Json.moshi.adapter(ActiveEndpoint::class.java)
//        val directAdapter: JsonAdapter<DirectEndpoint> =
//            Json.moshi.adapter(DirectEndpoint::class.java)
        val localAdapter: JsonAdapter<LocalEndpoint> =
            Json.moshi.adapter(LocalEndpoint::class.java)
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

    fun activePlayer(): Endpoint {
        return active(activePlayerKey) ?: LocalEndpoint.local
    }

    fun saveActivePlayer(id: EndpointId) {
        saveActive(id, activePlayerKey)
    }

    fun activeSource(): Endpoint {
        return active(activeSourceKey) ?: LocalEndpoint.local
    }

    fun saveActiveSource(id: EndpointId) {
        saveActive(id, activeSourceKey)
    }

    private fun active(key: String): CloudEndpoint? {
        return loadOpt(key, activeAdapter)?.let { id ->
            fetch().endpoints.find { e -> e.id == id.id }
                ?.let { e -> if (e is CloudEndpoint) e else null }
        }
    }

    private fun saveActive(id: EndpointId, key: String) {
        save(ActiveEndpoint(id), activeAdapter, key)
    }

    fun save(endpoint: Endpoint): Endpoints {
        val newValue =
            Endpoints(fetchCustom().endpoints.filterNot { e -> e.id == endpoint.id }.plus(endpoint))
        saveAll(newValue)
        return newValue
    }

    fun saveAll(endpoints: Endpoints) {
        save(endpoints, endpointsAdapter, endpointsKey)
    }

    fun remove(endpointId: EndpointId) {
        saveAll(fetchCustom().remove(endpointId))
    }

    fun fetch(): Endpoints = Endpoints(listOf(LocalEndpoint.local).plus(fetchCustom().endpoints))

    private fun fetchCustom(): Endpoints = load(endpointsKey, endpointsAdapter, Endpoints(emptyList()))

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
