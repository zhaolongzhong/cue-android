package com.example.cue.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

class JsonValue private constructor(private val value: Any?) {
    fun asBoolean(): Boolean? = value as? Boolean
    fun asString(): String? = value as? String
    fun asDouble(): Double? = (value as? Number)?.toDouble()
    fun asInt(): Int? = (value as? Number)?.toInt()
    fun asLong(): Long? = (value as? Number)?.toLong()
    fun asList(): List<JsonValue>? = (value as? List<*>)?.map { JsonValue(it) }
    fun asMap(): Map<String, JsonValue>? = (value as? Map<*, *>)?.mapValues { JsonValue(it.value) }?.mapKeys { it.key.toString() }
    fun isNull(): Boolean = value == null

    fun getRaw(): Any? = value

    companion object {
        fun of(value: Any?): JsonValue = JsonValue(value)
    }
}

class JsonValueAdapter : JsonAdapter<JsonValue>() {
    @FromJson
    override fun fromJson(reader: JsonReader): JsonValue = JsonValue.of(readValue(reader))

    @ToJson
    override fun toJson(writer: JsonWriter, value: JsonValue?) {
        writeValue(writer, value?.getRaw())
    }

    private fun readValue(reader: JsonReader): Any? = when (reader.peek()) {
        JsonReader.Token.NULL -> reader.nextNull()
        JsonReader.Token.BOOLEAN -> reader.nextBoolean()
        JsonReader.Token.NUMBER -> reader.nextDouble()
        JsonReader.Token.STRING -> reader.nextString()
        JsonReader.Token.BEGIN_ARRAY -> {
            val list = mutableListOf<Any?>()
            reader.beginArray()
            while (reader.hasNext()) {
                list.add(readValue(reader))
            }
            reader.endArray()
            list
        }
        JsonReader.Token.BEGIN_OBJECT -> {
            val map = mutableMapOf<String, Any?>()
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                val value = readValue(reader)
                map[name] = value
            }
            reader.endObject()
            map
        }
        else -> throw JsonDataException("Unexpected token: ${reader.peek()}")
    }

    private fun writeValue(writer: JsonWriter, value: Any?) {
        when (value) {
            null -> writer.nullValue()
            is Boolean -> writer.value(value)
            is Number -> writer.value(value)
            is String -> writer.value(value)
            is List<*> -> {
                writer.beginArray()
                value.forEach { writeValue(writer, it) }
                writer.endArray()
            }
            is Map<*, *> -> {
                writer.beginObject()
                value.forEach { (key, value) ->
                    writer.name(key.toString())
                    writeValue(writer, value)
                }
                writer.endObject()
            }
            else -> throw JsonDataException("Unsupported type: ${value.javaClass}")
        }
    }
}
