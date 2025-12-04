package pl.szymanski.wiktor.ta.domain

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledCompensatedEvent
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

// Note: These helpers could be moved to an infrastructure layer if modularization is introduced

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
                
            )
        is AccommodationBookedEvent ->
            this.copy(
                status = AccommodationStatusEnum.BOOKED,
                bookingId = event.bookingId,
                
            )
        is AccommodationBookingCanceledEvent ->
            this.copy(
                status = AccommodationStatusEnum.AVAILABLE,
                bookingId = null,
                
            )
        is AccommodationExpiredEvent ->
            this.copy(
                status = AccommodationStatusEnum.EXPIRED,
                
            )
        is AccommodationBookedCompensatedEvent ->
            this.copy(
                status = AccommodationStatusEnum.AVAILABLE,
                bookingId = null,

            )
        is AccommodationBookingCanceledCompensatedEvent -> {
            this.copy(
                status = AccommodationStatusEnum.BOOKED,
                bookingId = event.bookingId,
            )
        }
    }
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
                
            )
        is AttractionBookedEvent -> {
            val newBookings = this.bookings.toMutableList()
            newBookings.add(event.bookingId)
            this.copy(
                bookings = newBookings,
                
            )
        }
        is AttractionFullEvent ->
            this.copy(
                status = AttractionStatusEnum.FULL,
                
            )
        is AttractionAvailableEvent ->
            this.copy(
                status = AttractionStatusEnum.SCHEDULED,
                
            )
        is AttractionBookingCanceledEvent -> {
            val newBookings = this.bookings.toMutableList()
            newBookings.removeIf { it == event.bookingId }
            this.copy(
                bookings = newBookings,
                
            )
        }
        is AttractionExpiredEvent ->
            this.copy(
                status = AttractionStatusEnum.EXPIRED,
                
            )
        is AttractionBookedCompensatedEvent -> {
            val newBookings = this.bookings.toMutableList()
            newBookings.removeIf { it == event.bookingId }
            if (this.status == AttractionStatusEnum.FULL)
                this.copy(
                    bookings = newBookings,
                    status = AttractionStatusEnum.SCHEDULED,
                )
            else
                this.copy(
                    bookings = newBookings,
                )
        }
        is AttractionBookingCanceledCompensatedEvent -> {
            val newBookings = this.bookings.toMutableList()
            newBookings.add(event.bookingId)
            if (newBookings.size >= this.capacity)
                this.copy(
                    bookings = newBookings,
                    status = AttractionStatusEnum.FULL,
                )
            else
                this.copy(
                    bookings = newBookings,
                )

        }
    }
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
                
            )
        is CommuteBookedEvent -> {
            val newBookings = this.bookings.toMutableMap()
            newBookings[event.bookingId.toString()] = event.seat.toString()
            this.copy(
                bookings = newBookings,
                
            )
        }
        is CommuteFullEvent ->
            this.copy(
                status = CommuteStatusEnum.FULL,
                
            )
        is CommuteAvailableEvent ->
            this.copy(
                status = CommuteStatusEnum.SCHEDULED,
                
            )
        is CommuteBookingCanceledEvent -> {
            val newBookings = this.bookings.toMutableMap()
            newBookings.remove(event.bookingId.toString())
            this.copy(
                bookings = newBookings,
                
            )
        }
        is CommuteExpiredEvent ->
            this.copy(
                status = CommuteStatusEnum.EXPIRED,
                
            )

        is CommuteBookedCompensatedEvent -> {
            val newBookings = this.bookings.toMutableMap()
            newBookings.remove(event.bookingId.toString())
            if (this.status == CommuteStatusEnum.FULL)
                this.copy(
                    bookings = newBookings,
                    status = CommuteStatusEnum.SCHEDULED,
                )
            else
                this.copy(
                    bookings = newBookings,
                )
        }
        is CommuteBookingCanceledCompensatedEvent -> {
            val newBookings = this.bookings.toMutableMap()
            newBookings[event.bookingId.toString()] = event.seat.toString()
            if (newBookings.size >= this.seats.size)
                this.copy(
                    bookings = newBookings,
                    status = CommuteStatusEnum.FULL,
                )
            else
                this.copy(
                    bookings = newBookings,
                )
        }
    }
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
                
            )
        is TravelOfferMadeUnavailableEvent ->
            this.copy(
                status = TravelOfferStatusEnum.UNAVAILABLE,
                
            )
        is TravelOfferMadeAvailableEvent ->
            this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                
            )
        is TravelOfferExpiredEvent ->
            this.copy(
                status = TravelOfferStatusEnum.EXPIRED,
                
            )
        is TravelOfferReservedEvent ->
            this.copy(
                status = TravelOfferStatusEnum.RESERVED,
                bookingId = event.bookingId,
                
            )
        is TravelOfferBookedEvent ->
            this.copy(
                status = TravelOfferStatusEnum.BOOKED,
                bookingId = event.bookingId,
                
            )
        is TravelOfferReservationCanceledEvent ->
            this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null,
                
            )
        is TravelOfferReleaseEvent ->
            this.copy(
                status = TravelOfferStatusEnum.RELEASING,
                
            )
        is TravelOfferRebookedEvent ->
            this.copy(
                status = TravelOfferStatusEnum.BOOKED,
                
            )
        is TravelOfferBookingCanceledEvent ->
            this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null,
                
            )

        is TravelOfferBookedCompensatedEvent -> {
            this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null,
            )
        }
        is TravelOfferBookingCanceledCompensatedEvent -> {
            this.copy(
                status = TravelOfferStatusEnum.BOOKED,
                bookingId = event.bookingId,
            )
        }
    }
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

    // Apply all events to reconstruct the current state
    for ((event, revision) in events) {
        travelOffer = travelOffer.apply(event, revision)
    }

    return travelOffer
}
