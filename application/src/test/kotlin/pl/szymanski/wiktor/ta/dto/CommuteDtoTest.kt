package pl.szymanski.wiktor.ta.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.time.LocalDateTime
import java.util.UUID

class CommuteDtoTest {
    @Test
    fun `fromDomain maps fields and available seats`() {
        val dep = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.of(2025, 1, 1, 8, 0))
        val arr = LocationAndTime(LocationEnum.BERLIN, LocalDateTime.of(2025, 1, 1, 12, 30))
        val seat1 = Seat.Picked("A", "1")
        val seat2 = Seat.Picked("A", "2")
        val commute = Commute(
            name = "Morning Express",
            departure = dep,
            arrival = arr,
            seats = listOf(seat1, seat2),
            bookings = mutableMapOf(UUID.randomUUID() to seat1),
        )

        val dto = CommuteDto.fromDomain(commute)

        assertEquals(commute.id.toString(), dto.id)
        assertEquals(commute.name, dto.name)
        assertEquals(dep.location.name, dto.departure.location)
        assertEquals(dep.time.toString(), dto.departure.time)
        assertEquals(arr.location.name, dto.arrival.location)
        assertEquals(arr.time.toString(), dto.arrival.time)

        // Only seat2 should remain available
        assertEquals(1, dto.availableSeats.size)
        assertTrue(dto.availableSeats.contains(seat2.toString()))
    }
}
