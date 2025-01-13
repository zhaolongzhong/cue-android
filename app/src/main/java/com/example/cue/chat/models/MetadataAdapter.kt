package com.example.cue.chat.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

class MetadataAdapter : JsonAdapter<Map<String, Any>>() {
    @FromJson
    override fun fromJson(reader: JsonReader): Map<String, Any>? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }

        val result = mutableMapOf<String, Any>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            val value = when (reader.peek()) {
                JsonReader.Token.NULL -> reader.nextNull()
                JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                JsonReader.Token.NUMBER -> reader.nextDouble()
                JsonReader.Token.STRING -> reader.nextString()
                else -> reader.skipValue()
            }
            if (value != null) {
                result[name] = value
            }
        }
        reader.endObject()
        return result
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Map<String, Any>?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginObject()
        value.forEach { (key, value) ->
            writer.name(key)
            when (value) {
                null -> writer.nullValue()
                is Boolean -> writer.value(value)
                is Number -> writer.value(value)
                is String -> writer.value(value)
                else -> writer.nullValue() // Skip unsupported types
            }
        }
        writer.endObject()
    }
}