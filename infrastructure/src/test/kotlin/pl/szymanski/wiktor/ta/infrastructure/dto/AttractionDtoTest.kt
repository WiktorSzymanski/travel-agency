package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime
import java.util.UUID

class AttractionDtoTest {
    @Test
    fun `fromDomain maps all fields including capacity and bookings`() {
        val bookingId1 = BookingId.generate()
        val bookingId2 = BookingId.generate()
        val attraction = Attraction(
            name = "Museum Tour",
            location = LocationEnum.PARIS,
            date = LocalDateTime.of(2025, 5, 10, 10, 0),
            capacity = 5,
            bookings = mutableListOf(bookingId1, bookingId2),
            status = AttractionStatusEnum.SCHEDULED
        )

        val dto = AttractionDto.fromDomain(attraction, version = 3L) as AttractionDto.Present

        assertEquals(attraction.id.value.toString(), dto.id)
        assertEquals("Museum Tour", dto.name)
        assertEquals("PARIS", dto.location)
        assertEquals("2025-05-10T10:00", dto.date)
        assertEquals(5, dto.capacity)
        assertEquals(2, dto.bookings.size)
        assertEquals(bookingId1.value.toString(), dto.bookings[0])
        assertEquals(bookingId2.value.toString(), dto.bookings[1])
        assertEquals("SCHEDULED", dto.status)
        assertEquals(3L, dto.version)
    }

    @Test
    fun `fromDomain returns Empty for null attraction`() {
        val dto = AttractionDto.fromDomain(null)

        assertEquals(AttractionDto.Empty, dto)
    }

    @Test
    fun `toDomain correctly converts all fields`() {
        val attractionId = UUID.randomUUID()
        val bookingId1 = UUID.randomUUID()
        val bookingId2 = UUID.randomUUID()
        val bookingId3 = UUID.randomUUID()
        val date = LocalDateTime.of(2025, 8, 20, 14, 30)

        val dto = AttractionDto.Present(
            id = attractionId.toString(),
            name = "City Walking Tour",
            location = "LONDON",
            date = date.toString(),
            capacity = 10,
            bookings = listOf(bookingId1.toString(), bookingId2.toString(), bookingId3.toString()),
            status = "SCHEDULED",
            version = 7L
        )

        val attraction = dto.toDomain()

        assertEquals(attractionId, attraction.id.value)
        assertEquals("City Walking Tour", attraction.name)
        assertEquals(LocationEnum.LONDON, attraction.location)
        assertEquals(date, attraction.date)
        assertEquals(10, attraction.capacity)
        assertEquals(3, attraction.bookings.size)
        assertEquals(bookingId1, attraction.bookings[0].value)
        assertEquals(bookingId2, attraction.bookings[1].value)
        assertEquals(bookingId3, attraction.bookings[2].value)
        assertEquals(AttractionStatusEnum.SCHEDULED, attraction.status)
    }

    @Test
    fun `toDomain with empty bookings creates empty list`() {
        val dto = AttractionDto.Present(
            id = UUID.randomUUID().toString(),
            name = "Test Attraction",
            location = "ROME",
            date = LocalDateTime.of(2025, 9, 15, 9, 0).toString(),
            capacity = 20,
            bookings = emptyList(),
            status = "FULL"
        )

        val attraction = dto.toDomain()

        assertEquals(0, attraction.bookings.size)
        assertEquals(AttractionStatusEnum.FULL, attraction.status)
    }

    @Test
    fun `Empty toDomain returns null`() {
        val attraction = AttractionDto.Empty.toDomain()

        assertNull(attraction)
    }

    @Test
    fun `roundtrip fromDomain and toDomain preserves all data`() {
        val original = Attraction(
            name = "Roundtrip Test Attraction",
            location = LocationEnum.BERLIN,
            date = LocalDateTime.of(2025, 10, 5, 11, 30),
            capacity = 15,
            bookings = mutableListOf(
                BookingId.generate(),
                BookingId.generate(),
                BookingId.generate()
            ),
            status = AttractionStatusEnum.SCHEDULED
        )

        val dto = AttractionDto.fromDomain(original, version = 12L) as AttractionDto.Present
        val restored = dto.toDomain()

        assertEquals(original.name, restored.name)
        assertEquals(original.location, restored.location)
        assertEquals(original.date, restored.date)
        assertEquals(original.capacity, restored.capacity)
        assertEquals(original.bookings.size, restored.bookings.size)
        assertEquals(original.status, restored.status)
        // Verify booking IDs are preserved
        original.bookings.forEachIndexed { index, bookingId ->
            assertEquals(bookingId.value, restored.bookings[index].value)
        }
    }
}

