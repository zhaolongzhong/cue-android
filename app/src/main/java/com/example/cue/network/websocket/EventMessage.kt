package com.example.cue.network.websocket

import com.example.cue.network.JsonValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class EventMessageType {
    @Json(name = "generic")
    GENERIC,

    @Json(name = "user")
    USER,

    @Json(name = "assistant")
    ASSISTANT,

    @Json(name = "client_connect")
    CLIENT_CONNECT,

    @Json(name = "client_disconnect")
    CLIENT_DISCONNECT,

    @Json(name = "client_status")
    CLIENT_STATUS,

    @Json(name = "ping")
    PING,

    @Json(name = "pong")
    PONG,

    @Json(name = "error")
    ERROR,
}

@JsonClass(generateAdapter = true)
data class Metadata(
    val author: Author?,
    val model: String?,
) {
    @JsonClass(generateAdapter = true)
    data class Author(
        val role: String,
        val name: String?,
    )
}

@JsonClass(generateAdapter = true)
data class MessagePayload(
    val message: String?,
    val sender: String,
    val recipient: String?,
    @Json(name = "websocket_request_id")
    val websocketRequestId: String?,
    val metadata: Metadata?,
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "msg_id")
    val msgId: String?,
    val payload: Map<String, Any>?,
)

@JsonClass(generateAdapter = true)
data class EventMessage(
    val type: EventMessageType,
    val payload: MessagePayload,
    @Json(name = "client_id")
    val clientId: String?,
    val metadata: Metadata?,
    @Json(name = "websocket_request_id")
    val websocketRequestId: String?,
)

@JsonClass(generateAdapter = true)
data class ClientEventPayload(
    @Json(name = "message") val message: String? = null,
    @Json(name = "sender") val sender: String? = null,
    @Json(name = "recipient") val recipient: String? = null,
    @Json(name = "websocket_request_id") val websocketRequestId: String? = null,
    @Json(name = "metadata") val metadata: Metadata? = null,
    @Json(name = "client_id") val clientId: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "msg_id") val msgId: String? = null,
    @Json(name = "payload") val payload: JsonValue? = null,
)
