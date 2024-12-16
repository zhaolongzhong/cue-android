package com.example.cue.anthropic

import com.squareup.moshi.*
import java.lang.reflect.Type

class ContentBlockAdapter : JsonAdapter<ContentBlock>() {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val textBlockAdapter = moshi.adapter(TextBlock::class.java)
    private val toolUseBlockAdapter = moshi.adapter(ToolUseBlock::class.java)
    private val imageBlockAdapter = moshi.adapter(ImageBlock::class.java)

    override fun fromJson(reader: JsonReader): ContentBlock? {
        val jsonValue = reader.readJsonValue()
        if (jsonValue !is Map<*, *>) return null

        when (jsonValue["type"] as? String) {
            "text" -> {
                val json = moshi.adapter(Map::class.java).toJson(jsonValue)
                val textBlock = textBlockAdapter.fromJson(json) ?: return null
                return ContentBlock.Text(textBlock)
            }
            "tool_use" -> {
                val json = moshi.adapter(Map::class.java).toJson(jsonValue)
                val toolUseBlock = toolUseBlockAdapter.fromJson(json) ?: return null
                return ContentBlock.ToolUse(toolUseBlock)
            }
            "image" -> {
                val json = moshi.adapter(Map::class.java).toJson(jsonValue)
                val imageBlock = imageBlockAdapter.fromJson(json) ?: return null
                return ContentBlock.Image(imageBlock)
            }
        }
        return null
    }

    override fun toJson(writer: JsonWriter, value: ContentBlock?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        when (value) {
            is ContentBlock.Text -> textBlockAdapter.toJson(writer, value.block)
            is ContentBlock.ToolUse -> toolUseBlockAdapter.toJson(writer, value.block)
            is ContentBlock.Image -> imageBlockAdapter.toJson(writer, value.block)
        }
    }

    companion object {
        val FACTORY = object : JsonAdapter.Factory {
            override fun create(
                type: Type,
                annotations: Set<Annotation>,
                moshi: Moshi
            ): JsonAdapter<*>? {
                if (type == ContentBlock::class.java) {
                    return ContentBlockAdapter()
                }
                return null
            }
        }
    }
}