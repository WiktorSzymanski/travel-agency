package pl.szymanski.wiktor.ta.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime

class AttractionDtoTest {
    @Test
    fun `fromDomain maps fields and calculates available slots`() {
        val attraction = Attraction(
            name = "Museum Tour",
            location = LocationEnum.PARIS,
            date = LocalDateTime.of(2025, 5, 10, 10, 0),
            capacity = 5,
            bookings = mutableListOf(BookingId.generate(), BookingId.generate()),
        )

        val dto = AttractionDto.fromDomain(attraction) as AttractionDto.Present

        assertEquals(attraction.id.toString(), dto.id)
        assertEquals("Museum Tour", dto.name)
        assertEquals("PARIS", dto.location)
        assertEquals("2025-05-10T10:00", dto.date)
        assertEquals(3, dto.availableSlots)
    }
}
