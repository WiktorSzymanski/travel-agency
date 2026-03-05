//package pl.szymanski.wiktor.ta.infrastructure.document
//
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNull
//import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
//import pl.szymanski.wiktor.ta.domain.LocationEnum
//import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
//import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
//import java.time.LocalDateTime
//import java.util.UUID
//
//class AttractionDocumentTest {
//    @Test
//    fun `fromDomain maps all fields including capacity and bookings`() {
//        val bookingId1 = BookingId.generate()
//        val bookingId2 = BookingId.generate()
//        val attraction = Attraction(
//            name = "Museum Tour",
//            location = LocationEnum.PARIS,
//            date = LocalDateTime.of(2025, 5, 10, 10, 0),
//            capacity = 5,
//            bookings = mutableListOf(bookingId1, bookingId2),
//            status = AttractionStatusEnum.SCHEDULED
//        )
//
//        val document = AttractionDocument.fromDomain(attraction, version = 3L) as AttractionDocument.Present
//
//        assertEquals(attraction.id.value.toString(), document.id)
//        assertEquals("Museum Tour", document.name)
//        assertEquals("PARIS", document.location)
//        assertEquals("2025-05-10T10:00", document.date)
//        assertEquals(5, document.capacity)
//        assertEquals(2, document.bookings.size)
//        assertEquals(bookingId1.value.toString(), document.bookings[0])
//        assertEquals(bookingId2.value.toString(), document.bookings[1])
//        assertEquals("SCHEDULED", document.status)
//        assertEquals(3L, document.version)
//    }
//
//    @Test
//    fun `fromDomain returns Empty for null attraction`() {
//        val document = AttractionDocument.fromDomain(null)
//
//        assertEquals(AttractionDocument.Empty, document)
//    }
//
//    @Test
//    fun `toDomain correctly converts all fields`() {
//        val attractionId = UUID.randomUUID()
//        val bookingId1 = UUID.randomUUID()
//        val bookingId2 = UUID.randomUUID()
//        val bookingId3 = UUID.randomUUID()
//        val date = LocalDateTime.of(2025, 8, 20, 14, 30)
//
//        val document = AttractionDocument.Present(
//            id = attractionId.toString(),
//            name = "City Walking Tour",
//            location = "LONDON",
//            date = date.toString(),
//            capacity = 10,
//            bookings = listOf(bookingId1.toString(), bookingId2.toString(), bookingId3.toString()),
//            status = "SCHEDULED",
//            version = 7L
//        )
//
//        val attraction = document.toDomain()
//
//        assertEquals(attractionId, attraction.id.value)
//        assertEquals("City Walking Tour", attraction.name)
//        assertEquals(LocationEnum.LONDON, attraction.location)
//        assertEquals(date, attraction.date)
//        assertEquals(10, attraction.capacity)
//        assertEquals(3, attraction.bookings.size)
//        assertEquals(bookingId1, attraction.bookings[0].value)
//        assertEquals(bookingId2, attraction.bookings[1].value)
//        assertEquals(bookingId3, attraction.bookings[2].value)
//        assertEquals(AttractionStatusEnum.SCHEDULED, attraction.status)
//    }
//
//    @Test
//    fun `toDomain with empty bookings creates empty list`() {
//        val document = AttractionDocument.Present(
//            id = UUID.randomUUID().toString(),
//            name = "Test Attraction",
//            location = "ROME",
//            date = LocalDateTime.of(2025, 9, 15, 9, 0).toString(),
//            capacity = 20,
//            bookings = emptyList(),
//            status = "FULL"
//        )
//
//        val attraction = document.toDomain()
//
//        assertEquals(0, attraction.bookings.size)
//        assertEquals(AttractionStatusEnum.FULL, attraction.status)
//    }
//
//    @Test
//    fun `Empty toDomain returns null`() {
//        val attraction = AttractionDocument.Empty.toDomain()
//
//        assertNull(attraction)
//    }
//
//    @Test
//    fun `roundtrip fromDomain and toDomain preserves all data`() {
//        val original = Attraction(
//            name = "Roundtrip Test Attraction",
//            location = LocationEnum.BERLIN,
//            date = LocalDateTime.of(2025, 10, 5, 11, 30),
//            capacity = 15,
//            bookings = mutableListOf(
//                BookingId.generate(),
//                BookingId.generate(),
//                BookingId.generate()
//            ),
//            status = AttractionStatusEnum.SCHEDULED
//        )
//
//        val document = AttractionDocument.fromDomain(original, version = 12L) as AttractionDocument.Present
//        val restored = document.toDomain()
//
//        assertEquals(original.name, restored.name)
//        assertEquals(original.location, restored.location)
//        assertEquals(original.date, restored.date)
//        assertEquals(original.capacity, restored.capacity)
//        assertEquals(original.bookings.size, restored.bookings.size)
//        assertEquals(original.status, restored.status)
//        original.bookings.forEachIndexed { index, bookingId ->
//            assertEquals(bookingId.value, restored.bookings[index].value)
//        }
//    }
//}
//
