package pl.szymanski.wiktor.ta.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime
import java.util.UUID

class AccommodationDtoTest {
    @Test
    fun `fromDomain maps all fields`() {
        val rent = Rent(
            from = LocalDateTime.of(2025, 2, 1, 14, 0),
            till = LocalDateTime.of(2025, 2, 7, 10, 0),
        )
        val bookingId = BookingId.generate()
        val acc = Accommodation(
            name = "Hotel Plaza",
            location = LocationEnum.ROME,
            rent = rent,
            bookingId = bookingId,
            status = AccommodationStatusEnum.BOOKED,
        )

        val dto = AccommodationDto.fromDomain(acc)

        assertEquals(acc.id.toString(), dto.id)
        assertEquals("Hotel Plaza", dto.name)
        assertEquals("ROME", dto.location)
        assertEquals(rent.from.toString(), dto.rent.from)
        assertEquals(rent.till.toString(), dto.rent.till)
        assertEquals(bookingId.toString(), dto.booking)
        assertEquals("BOOKED", dto.status)
    }
}
