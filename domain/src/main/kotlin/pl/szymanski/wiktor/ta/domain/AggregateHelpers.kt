package pl.szymanski.wiktor.ta.domain

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import java.time.LocalDateTime
import java.util.UUID

// Those are meant to be taken out from the domain module, probably to infrastructure module

fun Booking.create(
    userId: UUID,
    seat: Seat? = null,
    travelOfferId: UUID,
): Pair<Booking, BookingCreatedEvent> {
    val booking =
        Booking(
            userId = userId,
            travelOfferId = travelOfferId,
            seat = seat,
        )

    val event =
        BookingCreatedEvent(
            bookingId = booking.id,
            travelOfferId = travelOfferId,
            userId = userId,
            seat = seat,
            state = booking.status,
        )

    return Pair(booking, event)
}

fun Accommodation.apply(
    event: AccommodationEvent,
    revision: Int,
): Accommodation {
    return when (event) {
        is AccommodationCreatedEvent ->
            this.copy(
                id = event.accommodationId,
                name = event.name,
                location = event.location,
                rent = event.rent,
                status = AccommodationStatusEnum.AVAILABLE,
                lastRevision = revision,
            )
        is AccommodationBookedEvent ->
            this.copy(
                status = AccommodationStatusEnum.BOOKED,
                bookingId = event.bookingId,
                lastRevision = revision,
            )
        is AccommodationBookingCanceledEvent ->
            this.copy(
                status = AccommodationStatusEnum.AVAILABLE,
                bookingId = null,
                lastRevision = revision,
            )
        is AccommodationExpiredEvent ->
            this.copy(
                status = AccommodationStatusEnum.EXPIRED,
                lastRevision = revision,
            )
        else -> this.copy(lastRevision = revision)
    }
}

fun Accommodation.create(
    name: String,
    location: LocationEnum,
    rent: Rent,
): Pair<Accommodation, AccommodationCreatedEvent> {
    val accommodation =
        Accommodation(
            name = name,
            location = location,
            rent = rent,
        )

    val event =
        AccommodationCreatedEvent(
            accommodationId = accommodation.id,
            name = name,
            location = location,
            rent = rent,
        )

    return accommodation to event
}

fun Accommodation.fromEvents(events: List<Pair<AccommodationEvent, Int>>): Accommodation? {
    if (events.isEmpty()) return null

    val (createdEvent, _) = events.first()

    require(createdEvent is AccommodationCreatedEvent) { "First event must be AccommodationCreatedEvent" }

    var accommodation =
        Accommodation(
            id = createdEvent.accommodationId,
            name = createdEvent.name,
            location = createdEvent.location,
            rent = createdEvent.rent,
        )

    for ((event, revision) in events) {
        accommodation = accommodation.apply(event, revision)
    }

    return accommodation
}

fun Attraction.apply(
    event: AttractionEvent,
    revision: Int,
): Attraction {
    return when (event) {
        is AttractionCreatedEvent ->
            this.copy(
                id = event.attractionId,
                name = event.name,
                location = event.location,
                date = event.date,
                capacity = event.capacity,
                status = AttractionStatusEnum.SCHEDULED,
                bookings = mutableListOf(),
                lastRevision = revision,
            )
        is AttractionBookedEvent -> {
            val newBookings = this.bookings.toMutableList()
            newBookings.add(event.bookingId)
            this.copy(
                bookings = newBookings,
                lastRevision = revision,
            )
        }
        is AttractionFullEvent ->
            this.copy(
                status = AttractionStatusEnum.FULL,
                lastRevision = revision,
            )
        is AttractionAvailableEvent ->
            this.copy(
                status = AttractionStatusEnum.SCHEDULED,
                lastRevision = revision,
            )
        is AttractionBookingCanceledEvent -> {
            val newBookings = this.bookings.toMutableList()
            newBookings.removeIf { it == event.bookingId }
            this.copy(
                bookings = newBookings,
                lastRevision = revision,
            )
        }
        is AttractionExpiredEvent ->
            this.copy(
                status = AttractionStatusEnum.EXPIRED,
                lastRevision = revision,
            )
        else -> this.copy(lastRevision = revision)
    }
}

fun Attraction.create(
    name: String,
    location: LocationEnum,
    date: LocalDateTime,
    capacity: Int,
): Pair<Attraction, List<AttractionCreatedEvent>> {
    val attraction =
        Attraction(
            name = name,
            location = location,
            date = date,
            capacity = capacity,
        )

    val event =
        AttractionCreatedEvent(
            attractionId = attraction.id,
            name = name,
            location = location,
            date = date,
            capacity = capacity,
        )

    return attraction to listOf(event)
}

fun Attraction.fromEvents(events: List<Pair<AttractionEvent, Int>>): Attraction? {
    if (events.isEmpty()) return null

    val (createdEvent, _) = events.first()

    require(createdEvent is AttractionCreatedEvent) { "First event must be AttractionCreatedEvent" }

    var attraction =
        Attraction(
            id = createdEvent.attractionId,
            name = createdEvent.name,
            location = createdEvent.location,
            date = createdEvent.date,
            capacity = createdEvent.capacity,
        )

    for ((event, revision) in events) {
        attraction = attraction.apply(event, revision)
    }

    return attraction
}

fun Commute.apply(
    event: CommuteEvent,
    revision: Int,
): Commute {
    return when (event) {
        is CommuteCreatedEvent ->
            this.copy(
                id = event.commuteId,
                name = event.name,
                departure = event.departure,
                arrival = event.arrival,
                seats = event.seats,
                status = CommuteStatusEnum.SCHEDULED,
                bookings = mutableMapOf(),
                lastRevision = revision,
            )
        is CommuteBookedEvent -> {
            val newBookings = this.bookings.toMutableMap()
            newBookings[event.bookingId.toString()] = event.seat.toString()
            this.copy(
                bookings = newBookings,
                lastRevision = revision,
            )
        }
        is CommuteFullEvent ->
            this.copy(
                status = CommuteStatusEnum.FULL,
                lastRevision = revision,
            )
        is CommuteAvailableEvent ->
            this.copy(
                status = CommuteStatusEnum.SCHEDULED,
                lastRevision = revision,
            )
        is CommuteBookingCanceledEvent -> {
            val newBookings = this.bookings.toMutableMap()
            newBookings.remove(event.bookingId.toString())
            this.copy(
                bookings = newBookings,
                lastRevision = revision,
            )
        }
        is CommuteExpiredEvent ->
            this.copy(
                status = CommuteStatusEnum.EXPIRED,
                lastRevision = revision,
            )
        else -> this.copy(lastRevision = revision)
    }
}

fun Commute.create(
    name: String,
    departure: LocationAndTime,
    arrival: LocationAndTime,
    seats: List<Seat>,
): Pair<Commute, List<CommuteCreatedEvent>> {
    val commute =
        Commute(
            name = name,
            departure = departure,
            arrival = arrival,
            seats = seats,
        )

    val event =
        CommuteCreatedEvent(
            commuteId = commute.id,
            name = name,
            departure = departure,
            arrival = arrival,
            seats = seats,
        )

    return commute to listOf(event)
}

fun Commute.fromEvents(events: List<Pair<CommuteEvent, Int>>): Commute? {
    if (events.isEmpty()) return null

    val (createdEvent, _) = events.first()

    require(createdEvent is CommuteCreatedEvent) { "First event must be CommuteCreatedEvent" }

    var commute =
        Commute(
            id = createdEvent.commuteId,
            name = createdEvent.name,
            departure = createdEvent.departure,
            arrival = createdEvent.arrival,
            seats = createdEvent.seats,
        )

    for ((event, revision) in events) {
        commute = commute.apply(event, revision)
    }

    return commute
}

fun TravelOffer.apply(
    event: TravelOfferEvent,
    revision: Int,
): TravelOffer {
    return when (event) {
        is TravelOfferCreatedEvent ->
            this.copy(
                id = event.travelOfferId,
                name = event.name,
                commuteId = event.commuteId,
                accommodationId = event.accommodationId,
                attractionId = event.attractionId,
                status = TravelOfferStatusEnum.AVAILABLE,
                lastRevision = revision,
            )
        is TravelOfferMadeUnavailableEvent ->
            this.copy(
                status = TravelOfferStatusEnum.UNAVAILABLE,
                lastRevision = revision,
            )
        is TravelOfferMadeAvailableEvent ->
            this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                lastRevision = revision,
            )
        is TravelOfferExpiredEvent ->
            this.copy(
                status = TravelOfferStatusEnum.EXPIRED,
                lastRevision = revision,
            )
        is TravelOfferReservedEvent ->
            this.copy(
                status = TravelOfferStatusEnum.RESERVED,
                bookingId = event.bookingId,
                lastRevision = revision,
            )
        is TravelOfferBookedEvent ->
            this.copy(
                status = TravelOfferStatusEnum.BOOKED,
                bookingId = event.bookingId,
                lastRevision = revision,
            )
        is TravelOfferReservationCanceledEvent ->
            this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null,
                lastRevision = revision,
            )
        is TravelOfferReleaseEvent ->
            this.copy(
                status = TravelOfferStatusEnum.RELEASING,
                lastRevision = revision,
            )
        is TravelOfferRebookedEvent ->
            this.copy(
                status = TravelOfferStatusEnum.BOOKED,
                lastRevision = revision,
            )
        is TravelOfferBookingCanceledEvent ->
            this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null,
                lastRevision = revision,
            )
        else -> this.copy(lastRevision = revision)
    }
}

fun TravelOffer.create(
    name: String,
    commuteId: UUID,
    accommodationId: UUID,
    attractionId: UUID? = null,
): Pair<TravelOffer, TravelOfferCreatedEvent> {
    val travelOffer =
        TravelOffer(
            id = UUID.randomUUID(),
            name = name,
            commuteId = commuteId,
            accommodationId = accommodationId,
            attractionId = attractionId,
        )

    val event =
        TravelOfferCreatedEvent(
            travelOfferId = travelOffer.id,
            name = name,
            commuteId = commuteId,
            accommodationId = accommodationId,
            attractionId = attractionId,
        )

    return travelOffer to event
}

fun TravelOffer.fromEvents(events: List<Pair<TravelOfferEvent, Int>>): TravelOffer? {
    if (events.isEmpty()) return null

    val (createdEvent, _) = events.first()

    require(createdEvent is TravelOfferCreatedEvent) { "First event must be TravelOfferCreatedEvent" }

    var travelOffer =
        TravelOffer(
            id = createdEvent.travelOfferId,
            name = createdEvent.name,
            commuteId = createdEvent.commuteId,
            accommodationId = createdEvent.accommodationId,
            attractionId = createdEvent.attractionId,
        )

    // Apply all events in order to reconstruct the current state
    for ((event, revision) in events) {
        travelOffer = travelOffer.apply(event, revision)
    }

    return travelOffer
}
