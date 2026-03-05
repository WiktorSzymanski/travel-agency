package pl.szymanski.wiktor.ta.infrastructure

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import com.fasterxml.jackson.databind.ObjectMapper
import pl.szymanski.wiktor.ta.Metadata
import java.util.UUID

class SerializeThat {
    private val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    @Test
    fun `event can be serialized and deserialized`() {
        val original = EventEnvelope2(
            CommuteDateMetEvent2(
                date = LocalDateTime.now(),
                commuteId = CommuteId2.generate()
            ),
            Metadata(UUID.randomUUID(), 0)
        )

        val json = mapper.writeValueAsString(original)
        val restored = mapper.readValue(json, EventEnvelope2::class.java)

        assertEquals(original, restored)
    }
}