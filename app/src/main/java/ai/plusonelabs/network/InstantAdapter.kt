package ai.plusonelabs.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.format.DateTimeFormatter

class InstantAdapter {
    @FromJson
    fun fromJson(string: String): Instant = Instant.parse(string)

    @ToJson
    fun toJson(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)
}
