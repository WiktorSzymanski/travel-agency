@file:Suppress("WildcardImport")

package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.exception.*
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccommodationTest {
    private lateinit var accommodationId: UUID
    private lateinit var bookingId: UUID
    private lateinit var now: LocalDateTime
    private lateinit var rentFuture: Rent
    private lateinit var rentPast: Rent
    private lateinit var accommodation: Accommodation

    @BeforeTest
    fun setup() {
        accommodationId = UUID.randomUUID()
        bookingId = UUID.randomUUID()
        now = LocalDateTime.now()
        rentFuture = Rent(from = now.plusSeconds(1), till = now.plusSeconds(5))
        rentPast = Rent(from = now.minusSeconds(5), till = now.minusSeconds(1))

        accommodation = Accommodation(accommodationId, "accommodation_name", LocationEnum.PARIS, rentFuture)
    }

    @Test
    fun aggregate_should_return_accommodation_and_created_event() {
        val rent = Rent(from = now.plusSeconds(10), till = now.plusSeconds(20))

        val (acc, events) = Accommodation.create(
            name = "accommodation_name",
            location = LocationEnum.PARIS,
            rent = rent,
        )

        assertEventEquals(
            AccommodationCreatedEvent(
                accommodationId = acc.id,
                name = "accommodation_name",
                location = LocationEnum.PARIS,
                rent = rent,
            ),
            events.first(),
        )
        assertEquals("accommodation_name", acc.name)
        assertEquals(LocationEnum.PARIS, acc.location)
        assertEquals(rent, acc.rent)
    }

    @Test
    fun book_should_succeed_when_available() {
        val events = accommodation.book(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationBookedEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(AccommodationStatusEnum.BOOKED, accommodation.status)
        assertEquals(bookingId, accommodation.bookingId)
    }

    @Test
    fun book_should_fail_when_not_available() {
        val accommodation = accommodation.copy(status = AccommodationStatusEnum.BOOKED)
        assertFailsWith<AccommodationBookingFailedException> { accommodation.book(bookingId) }
    }

    @Test
    fun cancelBooking_should_clear_booking_if_user_matches() {
        accommodation.book(bookingId)
        val events = accommodation.cancelBooking(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationBookingCanceledEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertNull(accommodation.bookingId)
    }

    @Test
    fun cancelBooking_should_fail_if_not_booked() {
        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.cancelBooking(bookingId) }
    }

    @Test
    fun cancelBooking_should_fail_if_wrong_user() {
        val randomBookingId = UUID.randomUUID()
        accommodation.book(randomBookingId)

        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.cancelBooking(bookingId) }
    }

    @Test
    fun book_after_rent_start_causes_automatic_expiration() {
        // move rent window to the past so statusCheck should mark it EXPIRED
        accommodation = accommodation.copy(rent = rentPast)

        assertFailsWith<AccommodationBookingFailedException> { accommodation.book(bookingId) }
        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
    }

    @Test
    fun cancelBooking_after_rent_start_causes_automatic_expiration() {
        // book first while still in future window
        accommodation.book(bookingId)
        // move rent to the past so statusCheck (called inside cancelBooking) expires it
        accommodation = accommodation.copy(rent = rentPast)

        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.cancelBooking(bookingId) }
        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
    }

    @Test
    fun book_should_fail_when_expired_and_not_change_state() {
        // statusCheck should early-return for non [AVAILABLE, BOOKED] statuses (EXPIRED here)
        accommodation = accommodation.copy(status = AccommodationStatusEnum.EXPIRED)
        val beforeBookingId = accommodation.bookingId

        assertFailsWith<AccommodationBookingFailedException> { accommodation.book(bookingId) }
        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
        assertEquals(beforeBookingId, accommodation.bookingId)
    }

    @Test
    fun cancelBooking_should_fail_when_expired_and_not_change_state() {
        // Prepare EXPIRED with a preset bookingId; statusCheck should early-return and not mutate state
        val presetBooking = UUID.randomUUID()
        accommodation = accommodation.copy(status = AccommodationStatusEnum.EXPIRED, bookingId = presetBooking)

        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.cancelBooking(presetBooking) }
        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
        assertEquals(presetBooking, accommodation.bookingId)
    }

    @Test
    fun expire_should_succeed_if_available_and_from_is_past() {
        val accommodation = accommodation.copy(rent = rentPast)
        val events = accommodation.expire()

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationExpiredEvent(
                accommodationId = accommodationId,
            ),
            events.first(),
        )
        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
    }

    @Test
    fun expire_should_fail_if_available_but_rent_date_not_met() {
        assertFailsWith<DomainException> { accommodation.expire() }
    }

    @Test
    fun expire_should_fail_if_in_unexpected_status() {
        val accommodation = accommodation.copy(status = AccommodationStatusEnum.BOOKED)

        assertFailsWith<DomainException> { accommodation.expire() }
    }

    @Test
    fun compensateBook_should_succeed_when_bookingId_matches() {
        accommodation.book(bookingId)
        val events = accommodation.compensateBook(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationBookingCanceledEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(AccommodationStatusEnum.AVAILABLE, accommodation.status)
        assertNull(accommodation.bookingId)
    }

    @Test
    fun compensateBook_should_fail_when_bookingId_does_not_match() {
        val randomBookingId = UUID.randomUUID()
        accommodation.book(randomBookingId)

        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.compensateBook(bookingId) }
    }

    @Test
    fun compensateBook_should_fail_when_no_booking_exists() {
        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.compensateBook(bookingId) }
    }

    @Test
    fun compensateCancelBooking_should_succeed_when_no_existing_booking() {
        val events = accommodation.compensateCancelBooking(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationBookedEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(AccommodationStatusEnum.BOOKED, accommodation.status)
        assertEquals(bookingId, accommodation.bookingId)
    }

    @Test
    fun compensateCancelBooking_should_fail_when_booking_already_exists() {
        val randomBookingId = UUID.randomUUID()
        accommodation.book(randomBookingId)

        assertFailsWith<AccommodationBookingFailedException> { accommodation.compensateCancelBooking(bookingId) }
    }

    @Test
    fun accommodation_apply_should_map_events_correctly() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val acc = Accommodation(id, "acc", LocationEnum.PARIS, Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(1)))

        // Created event is ignored by in-aggregate apply; state is set on creation or fromEvents
        acc.apply(AccommodationCreatedEvent(accommodationId = id, name = "n", location = LocationEnum.LONDON, rent = Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(2))))
        assertEquals(id, acc.id)
        assertEquals("acc", acc.name)
        assertEquals(LocationEnum.PARIS, acc.location)
        assertEquals(AccommodationStatusEnum.AVAILABLE, acc.status)

        acc.apply(AccommodationBookedEvent(accommodationId = id, bookingId = bookingId))
        assertEquals(AccommodationStatusEnum.BOOKED, acc.status)
        assertEquals(bookingId, acc.bookingId)

        acc.apply(AccommodationBookingCanceledEvent(accommodationId = id, bookingId = bookingId))
        assertEquals(AccommodationStatusEnum.AVAILABLE, acc.status)
        assertNull(acc.bookingId)

        acc.apply(AccommodationExpiredEvent(accommodationId = id))
        assertEquals(AccommodationStatusEnum.EXPIRED, acc.status)
    }

    @Test
    fun accommodation_fromEvents_should_handle_empty_and_invalid_first_event() {
        // empty -> null
        assertNull(Accommodation.fromEvents(emptyList()))

        // first not created -> throws
        val dummyId = UUID.randomUUID()
        val notCreatedFirst = listOf(AccommodationBookedEvent(accommodationId = dummyId, bookingId = UUID.randomUUID()))
        assertFailsWith<AccommodationMissingCreatedEventException> { Accommodation.fromEvents(notCreatedFirst) }
    }

    @Test
    fun accommodation_fromEvents_should_rebuild_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val events = listOf(
            AccommodationCreatedEvent(
                accommodationId = id,
                name = "acc",
                location = LocationEnum.ROME,
                rent = Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(3))
            ),
            AccommodationBookedEvent(accommodationId = id, bookingId = bookingId),
            AccommodationBookingCanceledEvent(accommodationId = id, bookingId = bookingId),
            AccommodationExpiredEvent(accommodationId = id),
        )

        val result = Accommodation.fromEvents(events)
        assertNotNull(result)
        assertEquals(id, result.id)
        assertEquals("acc", result.name)
        assertEquals(LocationEnum.ROME, result.location)
        assertEquals(AccommodationStatusEnum.EXPIRED, result.status)
        assertNull(result.bookingId)
    }
}
