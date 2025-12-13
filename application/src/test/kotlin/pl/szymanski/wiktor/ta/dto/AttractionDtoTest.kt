package pl.szymanski.wiktor.ta.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import java.time.LocalDateTime
import java.util.UUID

class AttractionDtoTest {
    @Test
    fun `fromDomain maps fields and calculates available slots`() {
        val attraction = Attraction(
            name = "Museum Tour",
            location = LocationEnum.PARIS,
            date = LocalDateTime.of(2025, 5, 10, 10, 0),
            capacity = 5,
            bookings = mutableListOf(UUID.randomUUID(), UUID.randomUUID()),
        )

        val dto = AttractionDto.fromDomain(attraction)

        assertEquals(attraction.id.toString(), dto.id)
        assertEquals("Museum Tour", dto.name)
        assertEquals("PARIS", dto.location)
        assertEquals("2025-05-10T10:00", dto.date)
        assertEquals(3, dto.availableSlots)
    }
}
