package pl.szymanski.wiktor.ta.infrastructure.repository

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.databind.SerializationFeature

object EventJsonSerializer {
    val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule()) // java.time support
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Use ISO-8601 for LocalDate/LocalDateTime
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)

    fun toBytes(event: Any): ByteArray =
        mapper.writeValueAsBytes(event)

    fun toJSON(event: Any): String =
        mapper.writeValueAsString(event)

    fun <T> fromBytes(bytes: ByteArray, clazz: Class<T>): T =
        mapper.readValue(bytes, clazz)

    fun <T> fromJSON(json: Any, clazz: Class<T>): T = when (json) {
        is String -> mapper.treeToValue(mapper.readTree(json), clazz)
        else -> mapper.readValue(mapper.writeValueAsString(json), clazz)
    }
}
