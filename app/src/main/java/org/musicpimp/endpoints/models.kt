package org.musicpimp.endpoints

import com.squareup.moshi.JsonClass
import org.musicpimp.CloudCredential
import org.musicpimp.EndpointId
import org.musicpimp.NonEmptyString

interface Endpoint {
    val id: EndpointId
    val name: NonEmptyString
}

interface EndpointInput<T : Endpoint> {
    fun withId(id: EndpointId): T
}

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
) : Endpoint {
    // For dropdowns
    override fun toString(): String = name.value
}

@JsonClass(generateAdapter = true)
data class LocalEndpoint(override val id: EndpointId, override val name: NonEmptyString): Endpoint {
    companion object {
        val local = LocalEndpoint(EndpointId("0"), NonEmptyString("This device"))
    }
    override fun toString(): String = name.value
}

@JsonClass(generateAdapter = true)
data class Endpoints(val endpoints: List<Endpoint>) {
    fun plus(endpoint: Endpoint) = Endpoints(endpoints.plus(endpoint))
    fun remove(id: EndpointId) = Endpoints(endpoints.filterNot { e -> e.id == id })
}

@JsonClass(generateAdapter = true)
data class ActiveEndpoint(val id: EndpointId)

//data class DirectEndpointInput(
//    val name: NonEmptyString,
//    val address: NonEmptyString,
//    val port: Int,
//    val creds: DirectCredential
//) : EndpointInput<DirectEndpoint> {
//    override fun withId(id: EndpointId): DirectEndpoint =
//        DirectEndpoint(id, name, address, port, creds)
//}

//@JsonClass(generateAdapter = true)
//data class DirectEndpoint(
//    override val id: EndpointId,
//    override val name: NonEmptyString,
//    val address: NonEmptyString,
//    val port: Int,
//    val creds: DirectCredential
//) : Endpoint
