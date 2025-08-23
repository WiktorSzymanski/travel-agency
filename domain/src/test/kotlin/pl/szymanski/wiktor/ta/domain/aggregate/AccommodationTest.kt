//package pl.szymanski.wiktor.ta.domain.aggregate
//
//import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
//import pl.szymanski.wiktor.ta.domain.LocationEnum
//import pl.szymanski.wiktor.ta.domain.Rent
//import pl.szymanski.wiktor.ta.domain.assertEventEquals
//import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
//import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
//import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
//import java.time.LocalDateTime
//import java.util.UUID
//import kotlin.test.BeforeTest
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNull
//
//class AccommodationTest {
//    private lateinit var accommodationId: UUID
//    private lateinit var bookingId: UUID
//    private lateinit var now: LocalDateTime
//    private lateinit var rentFuture: Rent
//    private lateinit var rentPast: Rent
//    private lateinit var accommodation: Accommodation
//
//    @BeforeTest
//    fun setup() {
//        accommodationId = UUID.randomUUID()
//        bookingId = UUID.randomUUID()
//        now = LocalDateTime.now()
//        rentFuture = Rent(from = now.plusSeconds(1), till = now.plusSeconds(5))
//        rentPast = Rent(from = now.minusSeconds(5), till = now.minusSeconds(1))
//
//        accommodation = Accommodation(accommodationId, "accommodation_name", LocationEnum.PARIS, rentFuture)
//    }
//
//    @Test
//    fun book_should_succeed_when_available() {
//        val event = accommodation.book(bookingId)
//
//        assertEventEquals(
//            AccommodationBookedEvent(
//                accommodationId = accommodationId,
//                bookingId = bookingId,
//            ),
//            event,
//        )
//        assertEquals(AccommodationStatusEnum.BOOKED, accommodation.status)
//        assertEquals(bookingId, accommodation.bookingId)
//    }
//
//    @Test
//    fun book_should_fail_when_not_available() {
//        val accommodation = accommodation.copy(status = AccommodationStatusEnum.BOOKED)
//
//        val event = accommodation.book(bookingId)
//
//        assertEventEquals(
//            AccommodationBookFailedEvent(
//                accommodationId = accommodationId,
//                bookingId = bookingId,
//                message = "Accommodation $accommodationId cannot be booked when in status ${AccommodationStatusEnum.BOOKED}",
//            ),
//            event,
//        )
//    }
//
//    @Test
//    fun cancelBooking_should_clear_booking_if_user_matches() {
//        accommodation.book(bookingId)
//        val event = accommodation.cancelBooking(bookingId)
//
//        assertEventEquals(
//            AccommodationBookingCanceledEvent(
//                accommodationId = accommodationId,
//                bookingId = bookingId,
//            ),
//            event,
//        )
//        assertNull(accommodation.bookingId)
//    }
//
//    @Test
//    fun cancelBooking_should_fail_if_not_booked() {
//        val event = accommodation.cancelBooking(bookingId)
//
//        assertEventEquals(
//            AccommodationBookingCancelFailedEvent(
//                accommodationId = accommodationId,
//                bookingId = bookingId,
//                message = "Accommodation $accommodationId booking cannot be canceled when in status ${AccommodationStatusEnum.AVAILABLE}",
//            ),
//            event,
//        )
//    }
//
//    @Test
//    fun cancelBooking_should_fail_if_wrong_user() {
//        val randomBookingId = UUID.randomUUID()
//        accommodation.book(randomBookingId)
//
//        val event = accommodation.cancelBooking(bookingId)
//
//        assertEventEquals(
//            AccommodationBookingCancelFailedEvent(
//                accommodationId = accommodationId,
//                bookingId = bookingId,
//                message = "Accommodation $accommodationId is not BOOKED by bookingId $bookingId",
//            ),
//            event,
//        )
//    }
//
//    @Test
//    fun expire_should_succeed_if_available_and_from_is_past() {
//        val accommodation = accommodation.copy(rent = rentPast)
//        val event = accommodation.expire()
//
//        assertEventEquals(
//            AccommodationExpiredEvent(
//                accommodationId = accommodationId,
//            ),
//            event,
//        )
//        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
//    }
//
//    @Test
//    fun expire_should_fail_if_available_but_rent_date_not_met() {
//        val event = accommodation.expire()
//
//        assertEventEquals(
//            AccommodationExpireFailedEvent(
//                accommodationId = accommodationId,
//                message = "Accommodation $accommodationId cannot be expired before its rent start",
//            ),
//            event,
//        )
//    }
//
//    @Test
//    fun expire_should_fail_if_in_unexpected_status() {
//        val accommodation = accommodation.copy(status = AccommodationStatusEnum.BOOKED)
//
//        val event = accommodation.expire()
//
//        assertEventEquals(
//            AccommodationExpireFailedEvent(
//                accommodationId = accommodationId,
//                message = "Accommodation $accommodationId cannot expire in status ${AccommodationStatusEnum.BOOKED}",
//            ),
//            event,
//        )
//    }
//}
