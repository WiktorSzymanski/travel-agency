//package pl.szymanski.wiktor.ta.infrastructure.document
//
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertFailsWith
//import kotlin.test.assertNull
//import pl.szymanski.wiktor.ta.domain.BookingState
//import pl.szymanski.wiktor.ta.domain.Seat
//import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
//import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
//import pl.szymanski.wiktor.ta.domain.aggregate.Booking
//import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
//import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
//import java.time.LocalDateTime
//import java.util.UUID
//
//class BookingDocumentTest {
//    @Test
//    fun `fromDomain maps fields with picked seat`() {
//        val userId = UUID.randomUUID()
//        val offer = TravelOffer(
//            CommuteId.generate(),
//            AccommodationId.generate(),
//            AttractionId.Empty,
//        )
//        val booking = Booking(
//            userId = userId,
//            travelOffer = offer,
//            seat = Seat.Picked("B", "3"),
//            status = BookingState.PROCESSING,
//            message = "processing",
//        )
//
//        val document = BookingDocument.fromDomain(booking, version = 4L)
//
//        assertEquals(booking.id.value.toString(), document.id)
//        assertEquals(userId.toString(), document.userId)
//        assertEquals(TravelOfferDocument.fromDomain(offer), document.travelOffer)
//        requireNotNull(document.seat)
//        assertEquals("B", document.seat.row)
//        assertEquals("3", document.seat.column)
//        assertEquals("PROCESSING", document.status)
//        assertEquals("processing", document.message)
//        assertEquals(booking.timestamp.toString(), document.timestamp)
//        assertEquals(4L, document.version)
//    }
//
//    @Test
//    fun `SeatDocument fromDomain throws for Seat Any`() {
//        val userId = UUID.randomUUID()
//        val offer = TravelOffer(
//            CommuteId.generate(),
//            AccommodationId.generate(),
//            AttractionId.Empty,
//        )
//        val booking = Booking(
//            userId = userId,
//            travelOffer = offer,
//            seat = Seat.Any,
//        )
//
//        val ex = assertFailsWith<IllegalArgumentException> {
//            BookingDocument.fromDomain(booking)
//        }
//
//        assertEquals("Seat Any cannot be serialized to Document, it should be Picked by now", ex.message)
//    }
//
//    @Test
//    fun `toDomain correctly converts all fields with picked seat`() {
//        val bookingId = UUID.randomUUID()
//        val userId = UUID.randomUUID()
//        val commuteId = UUID.randomUUID()
//        val accommodationId = UUID.randomUUID()
//        val attractionId = UUID.randomUUID()
//        val timestamp = LocalDateTime.of(2025, 6, 15, 10, 30)
//
//        val document = BookingDocument(
//            id = bookingId.toString(),
//            userId = userId.toString(),
//            travelOffer = TravelOfferDocument(
//                commuteId = commuteId.toString(),
//                accommodationId = accommodationId.toString(),
//                attractionId = attractionId.toString()
//            ),
//            seat = SeatDocument("C", "5"),
//            status = "BOOKED",
//            message = "Booking confirmed",
//            timestamp = timestamp.toString(),
//            version = 8L
//        )
//
//        val booking = document.toDomain()
//
//        assertEquals(bookingId, booking.id.value)
//        assertEquals(userId, booking.userId)
//        assertEquals(commuteId, booking.travelOffer.commuteId.value)
//        assertEquals(accommodationId, booking.travelOffer.accommodationId.value)
//        assertEquals(attractionId, booking.travelOffer.attractionId.value)
//        assertEquals(Seat.Picked("C", "5"), booking.seat)
//        assertEquals(BookingState.BOOKED, booking.status)
//        assertEquals("Booking confirmed", booking.message)
//        assertEquals(timestamp, booking.timestamp)
//    }
//
//    @Test
//    fun `toDomain with null seat creates Seat Any`() {
//        val document = BookingDocument(
//            id = UUID.randomUUID().toString(),
//            userId = UUID.randomUUID().toString(),
//            travelOffer = TravelOfferDocument(
//                commuteId = UUID.randomUUID().toString(),
//                accommodationId = UUID.randomUUID().toString(),
//                attractionId = UUID.randomUUID().toString()
//            ),
//            seat = null,
//            status = "NEW",
//            message = null,
//            timestamp = LocalDateTime.now().toString()
//        )
//
//        val booking = document.toDomain()
//
//        assertEquals(Seat.Any, booking.seat)
//        assertEquals(BookingState.NEW, booking.status)
//        assertNull(booking.message)
//    }
//
//    @Test
//    fun `roundtrip fromDomain and toDomain preserves all data`() {
//        val userId = UUID.randomUUID()
//        val original = Booking(
//            userId = userId,
//            travelOffer = TravelOffer(
//                CommuteId.generate(),
//                AccommodationId.generate(),
//                AttractionId.generate()
//            ),
//            seat = Seat.Picked("D", "10"),
//            status = BookingState.PROCESSING,
//            message = "Test message",
//            timestamp = LocalDateTime.of(2025, 12, 1, 15, 45)
//        )
//
//        val document = BookingDocument.fromDomain(original, version = 15L)
//        val restored = document.toDomain()
//
//        assertEquals(original.userId, restored.userId)
//        assertEquals(original.travelOffer.commuteId.value, restored.travelOffer.commuteId.value)
//        assertEquals(original.travelOffer.accommodationId.value, restored.travelOffer.accommodationId.value)
//        assertEquals(original.travelOffer.attractionId.value, restored.travelOffer.attractionId.value)
//        assertEquals(original.seat, restored.seat)
//        assertEquals(original.status, restored.status)
//        assertEquals(original.message, restored.message)
//        assertEquals(original.timestamp, restored.timestamp)
//    }
//}
//
