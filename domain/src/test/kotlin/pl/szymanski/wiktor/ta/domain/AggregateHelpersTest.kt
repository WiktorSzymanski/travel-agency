package pl.szymanski.wiktor.ta.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import java.time.LocalDateTime
import java.util.UUID

class AggregateHelpersTest {
    // Accommodation
    @Test
    fun accommodation_apply_should_map_events_correctly() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val acc = Accommodation(id, "acc", LocationEnum.PARIS, Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(1)))

        val createdApplied = acc.apply(
            AccommodationCreatedEvent(accommodationId = id, name = "n", location = LocationEnum.LONDON, rent = Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(2))),
            0,
        )
        assertEquals(id, createdApplied.id)
        assertEquals("n", createdApplied.name)
        assertEquals(LocationEnum.LONDON, createdApplied.location)
        assertEquals(AccommodationStatusEnum.AVAILABLE, createdApplied.status)

        val bookedApplied = createdApplied.apply(
            AccommodationBookedEvent(accommodationId = id, bookingId = bookingId),
            1,
        )
        assertEquals(AccommodationStatusEnum.BOOKED, bookedApplied.status)
        assertEquals(bookingId, bookedApplied.bookingId)

        val cancelApplied = bookedApplied.apply(
            AccommodationBookingCanceledEvent(accommodationId = id, bookingId = bookingId),
            2,
        )
        assertEquals(AccommodationStatusEnum.AVAILABLE, cancelApplied.status)
        assertNull(cancelApplied.bookingId)

        val expiredApplied = cancelApplied.apply(
            AccommodationExpiredEvent(accommodationId = id),
            3,
        )
        assertEquals(AccommodationStatusEnum.EXPIRED, expiredApplied.status)
    }

    @Test
    fun accommodation_fromEvents_should_handle_empty_and_invalid_first_event() {
        val dummy = Accommodation(UUID.randomUUID(), "d", LocationEnum.BERLIN, Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(1)))

        // empty -> null
        assertNull(dummy.fromEvents(emptyList()))

        // first not created -> throws
        val notCreatedFirst = listOf(AccommodationBookedEvent(accommodationId = dummy.id, bookingId = UUID.randomUUID()) to 0)
        assertFailsWith<IllegalArgumentException> { dummy.fromEvents(notCreatedFirst) }
    }

    @Test
    fun accommodation_fromEvents_should_rebuild_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val events = listOf(
            AccommodationCreatedEvent(accommodationId = id, name = "acc", location = LocationEnum.ROME, rent = Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(3))) to 0,
            AccommodationBookedEvent(accommodationId = id, bookingId = bookingId) to 1,
            AccommodationBookingCanceledEvent(accommodationId = id, bookingId = bookingId) to 2,
            AccommodationExpiredEvent(accommodationId = id) to 3,
        )

        val result = Accommodation(id, "x", LocationEnum.PARIS, Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(1))).fromEvents(events)
        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals("acc", result.name)
        assertEquals(LocationEnum.ROME, result.location)
        assertEquals(AccommodationStatusEnum.EXPIRED, result.status)
        assertNull(result.bookingId)
    }

    // Attraction
    @Test
    fun attraction_apply_should_map_events_correctly() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val base = Attraction(id, "a", LocationEnum.VENICE, LocalDateTime.now().plusDays(5), 2)

        val created = base.apply(AttractionCreatedEvent(attractionId = id, name = "n", location = LocationEnum.ZERMATT, date = LocalDateTime.now().plusDays(1), capacity = 3), 0)
        assertEquals(id, created.id)
        assertEquals("n", created.name)
        assertEquals(LocationEnum.ZERMATT, created.location)
        assertEquals(AttractionStatusEnum.SCHEDULED, created.status)
        assertEquals(0, created.bookings.size)

        val booked = created.apply(AttractionBookedEvent(attractionId = id, bookingId = bookingId), 1)
        assertEquals(listOf(bookingId), booked.bookings)

        val full = booked.apply(AttractionFullEvent(attractionId = id), 2)
        assertEquals(AttractionStatusEnum.FULL, full.status)

        val available = full.apply(AttractionAvailableEvent(attractionId = id), 3)
        assertEquals(AttractionStatusEnum.SCHEDULED, available.status)

        val canceled = available.apply(AttractionBookingCanceledEvent(attractionId = id, bookingId = bookingId), 4)
        assertEquals(0, canceled.bookings.size)

        val expired = canceled.apply(AttractionExpiredEvent(attractionId = id), 5)
        assertEquals(AttractionStatusEnum.EXPIRED, expired.status)
    }

    @Test
    fun attraction_fromEvents_should_handle_empty_and_invalid_first_event() {
        val dummy = Attraction(UUID.randomUUID(), "a", LocationEnum.VIENNA, LocalDateTime.now().plusDays(2), 10)
        assertNull(dummy.fromEvents(emptyList()))
        val invalid = listOf(AttractionBookedEvent(attractionId = dummy.id, bookingId = UUID.randomUUID()) to 0)
        assertFailsWith<IllegalArgumentException> { dummy.fromEvents(invalid) }
    }

    @Test
    fun attraction_fromEvents_should_rebuild_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val createdDate = LocalDateTime.now().plusDays(3)
        val events = listOf(
            AttractionCreatedEvent(attractionId = id, name = "tours", location = LocationEnum.PARIS, date = createdDate, capacity = 2) to 0,
            AttractionBookedEvent(attractionId = id, bookingId = bookingId) to 1,
            AttractionFullEvent(attractionId = id) to 2,
            AttractionBookingCanceledEvent(attractionId = id, bookingId = bookingId) to 3,
            AttractionAvailableEvent(attractionId = id) to 4,
        )

        val result = Attraction(id, "x", LocationEnum.LONDON, LocalDateTime.now().plusDays(1), 1).fromEvents(events)
        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals("tours", result.name)
        assertEquals(LocationEnum.PARIS, result.location)
        assertEquals(createdDate, result.date)
        assertEquals(2, result.capacity)
        assertEquals(AttractionStatusEnum.SCHEDULED, result.status)
        assertEquals(0, result.bookings.size)
    }

    // Commute
    @Test
    fun commute_apply_should_map_events_correctly() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val seat = Seat("1", "A")
        val base = Commute(id, "c", LocationAndTime(LocationEnum.MADRID, LocalDateTime.now()), LocationAndTime(LocationEnum.BARCELONA, LocalDateTime.now().plusHours(5)), listOf(seat))

        val created = base.apply(CommuteCreatedEvent(commuteId = id, name = "n", departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now()), arrival = LocationAndTime(LocationEnum.ROME, LocalDateTime.now().plusHours(3)), seats = listOf(seat)), 0)
        assertEquals(id, created.id)
        assertEquals("n", created.name)
        assertEquals(CommuteStatusEnum.SCHEDULED, created.status)
        assertEquals(0, created.bookings.size)

        val booked = created.apply(CommuteBookedEvent(commuteId = id, bookingId = bookingId, seat = seat), 1)
        assertEquals(mapOf(bookingId.toString() to seat.toString()), booked.bookings)

        val full = booked.apply(CommuteFullEvent(commuteId = id), 2)
        assertEquals(CommuteStatusEnum.FULL, full.status)

        val available = full.apply(CommuteAvailableEvent(commuteId = id), 3)
        assertEquals(CommuteStatusEnum.SCHEDULED, available.status)

        val canceled = available.apply(CommuteBookingCanceledEvent(commuteId = id, bookingId = bookingId, seat = seat), 4)
        assertEquals(0, canceled.bookings.size)

        val expired = canceled.apply(CommuteExpiredEvent(commuteId = id), 5)
        assertEquals(CommuteStatusEnum.EXPIRED, expired.status)
    }

    @Test
    fun commute_fromEvents_should_handle_empty_and_invalid_first_event() {
        val dummy = Commute(
            UUID.randomUUID(),
            "c",
            LocationAndTime(LocationEnum.BERLIN, LocalDateTime.now()),
            LocationAndTime(LocationEnum.BERLIN, LocalDateTime.now().plusHours(1)),
            emptyList(),
        )
        assertNull(dummy.fromEvents(emptyList()))
        val invalid = listOf(CommuteBookedEvent(commuteId = dummy.id, bookingId = UUID.randomUUID(), seat = Seat("1", "B")) to 0)
        assertFailsWith<IllegalArgumentException> { dummy.fromEvents(invalid) }
    }

    @Test
    fun commute_fromEvents_should_rebuild_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val seat = Seat("2", "C")
        val dep = LocationAndTime(LocationEnum.VALENCIA, LocalDateTime.now())
        val arr = LocationAndTime(LocationEnum.MARSEILLE, LocalDateTime.now().plusHours(2))
        val events = listOf(
            CommuteCreatedEvent(commuteId = id, name = "comm", departure = dep, arrival = arr, seats = listOf(seat)) to 0,
            CommuteBookedEvent(commuteId = id, bookingId = bookingId, seat = seat) to 1,
            CommuteBookingCanceledEvent(commuteId = id, bookingId = bookingId, seat = seat) to 2,
        )

        val result = Commute(id, "x", dep, arr, listOf(seat)).fromEvents(events)
        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals("comm", result.name)
        assertEquals(dep, result.departure)
        assertEquals(arr, result.arrival)
        assertEquals(0, result.bookings.size)
        assertEquals(CommuteStatusEnum.SCHEDULED, result.status)
    }

    // TravelOffer
    @Test
    fun travelOffer_apply_should_map_events_correctly() {
        val id = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val comId = UUID.randomUUID()
        val attrId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()

        val base = TravelOffer(id, "t", comId, accId, attrId)

        val created = base.apply(TravelOfferCreatedEvent(travelOfferId = id, name = "X", commuteId = comId, accommodationId = accId, attractionId = attrId), 0)
        assertEquals(id, created.id)
        assertEquals("X", created.name)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, created.status)

        val unavailable = created.apply(TravelOfferMadeUnavailableEvent(travelOfferId = id), 1)
        assertEquals(TravelOfferStatusEnum.UNAVAILABLE, unavailable.status)

        val available = unavailable.apply(TravelOfferMadeAvailableEvent(travelOfferId = id), 2)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, available.status)

        val reserved = available.apply(TravelOfferReservedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = null), 3)
        assertEquals(TravelOfferStatusEnum.RESERVED, reserved.status)
        assertEquals(bookingId, reserved.bookingId)

        val booked = reserved.apply(TravelOfferBookedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = null), 4)
        assertEquals(TravelOfferStatusEnum.BOOKED, booked.status)
        assertEquals(bookingId, booked.bookingId)

        val release = booked.apply(TravelOfferReleaseEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = null), 5)
        assertEquals(TravelOfferStatusEnum.RELEASING, release.status)

        val rebooked = release.apply(TravelOfferRebookedEvent(travelOfferId = id, bookingId = bookingId), 6)
        assertEquals(TravelOfferStatusEnum.BOOKED, rebooked.status)

        val canceled = rebooked.apply(TravelOfferBookingCanceledEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = null), 7)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, canceled.status)
        assertNull(canceled.bookingId)

        val expired = canceled.apply(TravelOfferExpiredEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId), 8)
        assertEquals(TravelOfferStatusEnum.EXPIRED, expired.status)
    }

    @Test
    fun travelOffer_fromEvents_should_handle_empty_and_invalid_first_event() {
        val dummy = TravelOffer(UUID.randomUUID(), "t", UUID.randomUUID(), UUID.randomUUID(), null)
        assertNull(dummy.fromEvents(emptyList()))
        val invalid = listOf(TravelOfferReservedEvent(travelOfferId = dummy.id, accommodationId = dummy.accommodationId, commuteId = dummy.commuteId, attractionId = dummy.attractionId, bookingId = UUID.randomUUID(), seat = null) to 0)
        assertFailsWith<IllegalArgumentException> { dummy.fromEvents(invalid) }
    }

    @Test
    fun travelOffer_fromEvents_should_rebuild_state() {
        val id = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val comId = UUID.randomUUID()
        val attrId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()

        val events = listOf(
            TravelOfferCreatedEvent(travelOfferId = id, name = "offer", commuteId = comId, accommodationId = accId, attractionId = attrId) to 0,
            TravelOfferReservedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = null) to 1,
            TravelOfferBookedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = null) to 2,
            TravelOfferBookingCanceledEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = null) to 3,
            TravelOfferMadeUnavailableEvent(travelOfferId = id) to 4,
            TravelOfferMadeAvailableEvent(travelOfferId = id) to 5,
        )

        val result = TravelOffer(id, "x", comId, accId, attrId).fromEvents(events)
        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals("offer", result.name)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, result.status)
        assertNull(result.bookingId)
        assertEquals(accId, result.accommodationId)
        assertEquals(comId, result.commuteId)
        assertEquals(attrId, result.attractionId)
    }

    // Compensation branches (apply)
    @Test
    fun accommodation_apply_compensation_events_should_update_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val base = Accommodation(id, "acc", LocationEnum.PARIS, Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(1)))

        // Given currently booked -> applying BookedCompensated should free it
        val booked = base.apply(AccommodationBookedEvent(accommodationId = id, bookingId = bookingId), 0)
        val compensated = booked.apply(AccommodationBookedCompensatedEvent(correlationId = null, accommodationId = id, bookingId = bookingId), 1)
        assertEquals(AccommodationStatusEnum.AVAILABLE, compensated.status)
        assertNull(compensated.bookingId)

        // Applying BookingCanceledCompensated should set it back to booked
        val backToBooked = compensated.apply(AccommodationBookingCanceledCompensatedEvent(correlationId = null, accommodationId = id, bookingId = bookingId), 2)
        assertEquals(AccommodationStatusEnum.BOOKED, backToBooked.status)
        assertEquals(bookingId, backToBooked.bookingId)
    }

    @Test
    fun attraction_apply_compensation_events_should_update_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val created = Attraction(id, "a", LocationEnum.VENICE, LocalDateTime.now().plusDays(1), 1)
            .apply(AttractionCreatedEvent(attractionId = id, name = "a", location = LocationEnum.VENICE, date = LocalDateTime.now().plusDays(1), capacity = 1), 0)

        // Fill to FULL
        val booked = created.apply(AttractionBookedEvent(attractionId = id, bookingId = bookingId), 1)
        val full = booked.apply(AttractionFullEvent(attractionId = id), 2)
        assertEquals(AttractionStatusEnum.FULL, full.status)

        // Applying BookedCompensated removes booking and makes it SCHEDULED again
        val compensated = full.apply(AttractionBookedCompensatedEvent(correlationId = null, attractionId = id, bookingId = bookingId), 3)
        assertEquals(0, compensated.bookings.size)
        assertEquals(AttractionStatusEnum.SCHEDULED, compensated.status)

        // Applying BookingCanceledCompensated adds a booking and makes it FULL (capacity=1)
        val backToFull = compensated.apply(AttractionBookingCanceledCompensatedEvent(correlationId = null, attractionId = id, bookingId = bookingId), 4)
        assertEquals(1, backToFull.bookings.size)
        assertEquals(AttractionStatusEnum.FULL, backToFull.status)
    }

    @Test
    fun commute_apply_compensation_events_should_update_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val seat = Seat("1", "A")
        val created = Commute(id, "c", LocationAndTime(LocationEnum.MADRID, LocalDateTime.now()), LocationAndTime(LocationEnum.BARCELONA, LocalDateTime.now().plusHours(1)), listOf(seat))
            .apply(CommuteCreatedEvent(commuteId = id, name = "c", departure = LocationAndTime(LocationEnum.MADRID, LocalDateTime.now()), arrival = LocationAndTime(LocationEnum.BARCELONA, LocalDateTime.now().plusHours(1)), seats = listOf(seat)), 0)

        // Fill to FULL
        val booked = created.apply(CommuteBookedEvent(commuteId = id, bookingId = bookingId, seat = seat), 1)
        val full = booked.apply(CommuteFullEvent(commuteId = id), 2)
        assertEquals(CommuteStatusEnum.FULL, full.status)

        // Applying BookedCompensated removes booking and makes Scheduled
        val compensated = full.apply(CommuteBookedCompensatedEvent(correlationId = null, commuteId = id, bookingId = bookingId, seat = seat), 3)
        assertEquals(0, compensated.bookings.size)
        assertEquals(CommuteStatusEnum.SCHEDULED, compensated.status)

        // Applying BookingCanceledCompensated adds back booking and becomes FULL again (seats=1)
        val backToFull = compensated.apply(CommuteBookingCanceledCompensatedEvent(correlationId = null, commuteId = id, bookingId = bookingId, seat = seat), 4)
        assertEquals(1, backToFull.bookings.size)
        assertEquals(CommuteStatusEnum.FULL, backToFull.status)
    }

    @Test
    fun travelOffer_apply_compensation_events_should_update_state() {
        val id = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val comId = UUID.randomUUID()
        val attrId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val seat = Seat("2", "B")

        val created = TravelOffer(id, "t", comId, accId, attrId)
            .apply(TravelOfferCreatedEvent(travelOfferId = id, name = "t", commuteId = comId, accommodationId = accId, attractionId = attrId), 0)
        val booked = created.apply(TravelOfferBookedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = seat), 1)
        assertEquals(TravelOfferStatusEnum.BOOKED, booked.status)

        // Applying BookedCompensated should free booking and set AVAILABLE
        val compensated = booked.apply(TravelOfferBookedCompensatedEvent(correlationId = null, travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = seat), 2)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, compensated.status)
        assertNull(compensated.bookingId)

        // Applying BookingCanceledCompensated should go back to BOOKED
        val backToBooked = compensated.apply(TravelOfferBookingCanceledCompensatedEvent(correlationId = null, travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = seat), 3)
        assertEquals(TravelOfferStatusEnum.BOOKED, backToBooked.status)
        assertEquals(bookingId, backToBooked.bookingId)
    }
}
