package com.example.cue.network.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EventMessage(
    val type: EventType,
    @Json(name = "client_status") val clientStatus: ClientStatus? = null,
    val payload: Any? = null
)

enum class EventType {
    @Json(name = "client_connect") CLIENT_CONNECT,
    @Json(name = "client_disconnect") CLIENT_DISCONNECT,
    @Json(name = "client_status") CLIENT_STATUS,
    @Json(name = "assistant") ASSISTANT,
    @Json(name = "user") USER,
    @Json(name = "generic") GENERIC,
    @Json(name = "error") ERROR
}

@JsonClass(generateAdapter = true)
data class ClientStatus(
    @Json(name = "client_id") val clientId: String,
    val status: String,
    @Json(name = "connected_at") val connectedAt: Long? = null,
    @Json(name = "disconnected_at") val disconnectedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class MessagePayload(
    val message: String,
    val sender: String? = null,
    val recipient: String? = null,
    @Json(name = "websocket_request_id") val websocketRequestId: String? = null,
    val metadata: Map<String, Any>? = null,
    @Json(name = "user_id") val userId: String? = null,
    val payload: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GenericPayload(
    val message: String? = null,
    val code: String? = null,
    val data: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ClientEvent(
    val type: EventType,
    val payload: MessagePayload
)