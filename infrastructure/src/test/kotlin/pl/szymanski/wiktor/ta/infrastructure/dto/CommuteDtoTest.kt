package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.time.LocalDateTime
import java.util.UUID

class CommuteDtoTest {
    @Test
    fun `fromDomain maps all fields including seats and bookings`() {
        val dep = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.of(2025, 1, 1, 8, 0))
        val arr = LocationAndTime(LocationEnum.BERLIN, LocalDateTime.of(2025, 1, 1, 12, 30))
        val seat1 = Seat.Picked("A", "1")
        val seat2 = Seat.Picked("A", "2")
        val seat3 = Seat.Picked("B", "1")
        val bookingId = BookingId.generate()
        val commute = Commute(
            name = "Morning Express",
            departure = dep,
            arrival = arr,
            seats = listOf(seat1, seat2, seat3),
            bookings = mutableMapOf(bookingId to seat1),
            status = CommuteStatusEnum.SCHEDULED
        )

        val dto = CommuteDto.fromDomain(commute, version = 5L)

        assertEquals(commute.id.value.toString(), dto.id)
        assertEquals(commute.name, dto.name)
        assertEquals(dep.location.name, dto.departure.location)
        assertEquals(dep.time.toString(), dto.departure.time)
        assertEquals(arr.location.name, dto.arrival.location)
        assertEquals(arr.time.toString(), dto.arrival.time)
        assertEquals(3, dto.seats.size)
        assertTrue(dto.seats.contains(seat1.toString()))
        assertTrue(dto.seats.contains(seat2.toString()))
        assertTrue(dto.seats.contains(seat3.toString()))
        assertEquals(1, dto.bookings.size)
        assertEquals(seat1.toString(), dto.bookings[bookingId.value.toString()])
        assertEquals("SCHEDULED", dto.status)
        assertEquals(5L, dto.version)
    }

    @Test
    fun `toDomain correctly converts all fields`() {
        val depTime = LocalDateTime.of(2025, 3, 15, 9, 30)
        val arrTime = LocalDateTime.of(2025, 3, 15, 14, 45)
        val commuteId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()

        val dto = CommuteDto(
            id = commuteId.toString(),
            name = "Express Train",
            departure = LocationAndTimeDto("LONDON", depTime.toString()),
            arrival = LocationAndTimeDto("PARIS", arrTime.toString()),
            seats = listOf("Picked(A, 1)", "Picked(A, 2)", "Picked(B, 1)"),
            bookings = mapOf(bookingId.toString() to "Picked(A, 1)"),
            status = "SCHEDULED",
            version = 3L
        )

        val commute = dto.toDomain()

        assertEquals(commuteId, commute.id.value)
        assertEquals("Express Train", commute.name)
        assertEquals(LocationEnum.LONDON, commute.departure.location)
        assertEquals(depTime, commute.departure.time)
        assertEquals(LocationEnum.PARIS, commute.arrival.location)
        assertEquals(arrTime, commute.arrival.time)
        assertEquals(3, commute.seats.size)
        assertTrue(commute.seats.contains(Seat.Picked("A", "1")))
        assertTrue(commute.seats.contains(Seat.Picked("A", "2")))
        assertTrue(commute.seats.contains(Seat.Picked("B", "1")))
        assertEquals(1, commute.bookings.size)
        assertEquals(Seat.Picked("A", "1"), commute.bookings[BookingId.from(bookingId)])
        assertEquals(CommuteStatusEnum.SCHEDULED, commute.status)
    }

    @Test
    fun `toDomain handles Seat Any correctly`() {
        val dto = CommuteDto(
            id = UUID.randomUUID().toString(),
            name = "Test",
            departure = LocationAndTimeDto("LONDON", LocalDateTime.now().toString()),
            arrival = LocationAndTimeDto("PARIS", LocalDateTime.now().toString()),
            seats = listOf("Any", "Picked(A, 1)"),
            bookings = emptyMap(),
            status = "SCHEDULED"
        )

        val commute = dto.toDomain()

        assertEquals(2, commute.seats.size)
        assertTrue(commute.seats.contains(Seat.Any))
        assertTrue(commute.seats.contains(Seat.Picked("A", "1")))
    }

    @Test
    fun `toDomain with empty bookings creates empty map`() {
        val dto = CommuteDto(
            id = UUID.randomUUID().toString(),
            name = "Test",
            departure = LocationAndTimeDto("BERLIN", LocalDateTime.now().toString()),
            arrival = LocationAndTimeDto("ROME", LocalDateTime.now().toString()),
            seats = listOf("Picked(C, 5)"),
            bookings = emptyMap(),
            status = "FULL"
        )

        val commute = dto.toDomain()

        assertTrue(commute.bookings.isEmpty())
        assertEquals(CommuteStatusEnum.FULL, commute.status)
    }

    @Test
    fun `roundtrip fromDomain and toDomain preserves all data`() {
        val original = Commute(
            name = "Roundtrip Test",
            departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.of(2025, 6, 1, 10, 0)),
            arrival = LocationAndTime(LocationEnum.BERLIN, LocalDateTime.of(2025, 6, 1, 15, 30)),
            seats = listOf(Seat.Picked("A", "1"), Seat.Picked("A", "2"), Seat.Any),
            bookings = mutableMapOf(
                BookingId.generate() to Seat.Picked("A", "1")
            ),
            status = CommuteStatusEnum.SCHEDULED
        )

        val dto = CommuteDto.fromDomain(original, version = 7L)
        val restored = dto.toDomain()

        assertEquals(original.name, restored.name)
        assertEquals(original.departure.location, restored.departure.location)
        assertEquals(original.departure.time, restored.departure.time)
        assertEquals(original.arrival.location, restored.arrival.location)
        assertEquals(original.arrival.time, restored.arrival.time)
        assertEquals(original.seats.size, restored.seats.size)
        assertEquals(original.bookings.size, restored.bookings.size)
        assertEquals(original.status, restored.status)
    }
}


