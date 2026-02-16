package pl.szymanski.wiktor.ta.infrastructure.dto

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

        val dto = AccommodationDto.fromDomain(acc, version = 2L)

        assertEquals(acc.id.value.toString(), dto.id)
        assertEquals("Hotel Plaza", dto.name)
        assertEquals("ROME", dto.location)
        assertEquals(rent.from.toString(), dto.rent.from)
        assertEquals(rent.till.toString(), dto.rent.till)
        assertEquals(bookingId.value.toString(), dto.booking)
        assertEquals("BOOKED", dto.status)
        assertEquals(2L, dto.version)
    }

    @Test
    fun `toDomain correctly converts all fields with booking`() {
        val accommodationId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val from = LocalDateTime.of(2025, 4, 10, 15, 0)
        val till = LocalDateTime.of(2025, 4, 15, 11, 0)

        val dto = AccommodationDto(
            id = accommodationId.toString(),
            name = "Grand Hotel",
            location = "PARIS",
            rent = RentDto(from.toString(), till.toString()),
            booking = bookingId.toString(),
            status = "BOOKED",
            version = 5L
        )

        val accommodation = dto.toDomain()

        assertEquals(accommodationId, accommodation.id.value)
        assertEquals("Grand Hotel", accommodation.name)
        assertEquals(LocationEnum.PARIS, accommodation.location)
        assertEquals(from, accommodation.rent.from)
        assertEquals(till, accommodation.rent.till)
        assertEquals(bookingId, accommodation.bookingId.value)
        assertEquals(AccommodationStatusEnum.BOOKED, accommodation.status)
    }

    @Test
    fun `toDomain handles null booking as Empty`() {
        val dto = AccommodationDto(
            id = UUID.randomUUID().toString(),
            name = "Test Hotel",
            location = "LONDON",
            rent = RentDto(
                LocalDateTime.of(2025, 5, 1, 12, 0).toString(),
                LocalDateTime.of(2025, 5, 5, 10, 0).toString()
            ),
            booking = null,
            status = "AVAILABLE"
        )

        val accommodation = dto.toDomain()

        assertEquals(BookingId.Empty, accommodation.bookingId)
        assertEquals(AccommodationStatusEnum.AVAILABLE, accommodation.status)
    }

    @Test
    fun `roundtrip fromDomain and toDomain preserves all data`() {
        val original = Accommodation(
            name = "Test Accommodation",
            location = LocationEnum.BERLIN,
            rent = Rent(
                from = LocalDateTime.of(2025, 7, 1, 14, 0),
                till = LocalDateTime.of(2025, 7, 10, 11, 0)
            ),
            bookingId = BookingId.generate(),
            status = AccommodationStatusEnum.BOOKED
        )

        val dto = AccommodationDto.fromDomain(original, version = 10L)
        val restored = dto.toDomain()

        assertEquals(original.name, restored.name)
        assertEquals(original.location, restored.location)
        assertEquals(original.rent.from, restored.rent.from)
        assertEquals(original.rent.till, restored.rent.till)
        assertEquals(original.bookingId.value, restored.bookingId.value)
        assertEquals(original.status, restored.status)
    }
}


