package pl.szymanski.wiktor.ta.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import java.util.UUID

class BookingDtoTest {
    @Test
    fun `fromDomain maps fields with picked seat`() {
        val userId = UUID.randomUUID()
        val offerId = TravelOfferId.from(UUID.randomUUID())
        val booking = Booking(
            userId = userId,
            travelOfferId = offerId,
            seat = Seat.Picked("B", "3"),
            status = BookingState.PROCESSING,
            message = "processing",
        )

        val dto = BookingDto.fromDomain(booking)

        assertEquals(booking.id.toString(), dto.id)
        assertEquals(userId.toString(), dto.userId)
        assertEquals(offerId.toString(), dto.travelOfferId)
        requireNotNull(dto.seat)
        assertEquals("B", dto.seat.row)
        assertEquals("3", dto.seat.column)
        assertEquals("PROCESSING", dto.status)
        assertEquals("processing", dto.message)
        assertEquals(booking.timestamp.toString(), dto.timestamp)
    }

    @Test
    fun `SeatDto fromDomain throws for Seat Any`() {
        val userId = UUID.randomUUID()
        val offerId = TravelOfferId.from(UUID.randomUUID())
        val booking = Booking(
            userId = userId,
            travelOfferId = offerId,
            seat = Seat.Any,
        )

        val ex = assertFailsWith<IllegalArgumentException> {
            BookingDto.fromDomain(booking)
        }

        assertEquals("Seat Any cannot be serialized to DTO, it should be Picked by now", ex.message)
    }
}
