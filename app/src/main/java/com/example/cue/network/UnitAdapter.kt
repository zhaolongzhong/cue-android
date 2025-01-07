package com.example.cue.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

class UnitAdapter : JsonAdapter<Unit>() {
    override fun fromJson(reader: JsonReader) {
        reader.skipValue()
        return Unit
    }

    override fun toJson(writer: JsonWriter, value: Unit?) {
        writer.nullValue()
    }
}
