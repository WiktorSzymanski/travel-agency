package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMakeAvailableFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMakeUnavailableFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookCompleteFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseCompleteFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReserveFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import java.util.UUID

data class TravelOffer(
    val _id: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
    var bookingId: UUID? = null,
    var status: TravelOfferStatusEnum = TravelOfferStatusEnum.AVAILABLE,
    val version: Int = 1,
) {
    fun apply(event: TravelOfferEvent): TravelOffer {
        return when (event) {
            is TravelOfferCreatedEvent -> this.copy(
                _id = event.travelOfferId,
                name = event.name,
                commuteId = event.commuteId,
                accommodationId = event.accommodationId,
                attractionId = event.attractionId,
                status = TravelOfferStatusEnum.AVAILABLE
            )
            is TravelOfferMadeUnavailableEvent -> this.copy(
                status = TravelOfferStatusEnum.UNAVAILABLE
            )
            is TravelOfferMadeAvailableEvent -> this.copy(
                status = TravelOfferStatusEnum.AVAILABLE
            )
            is TravelOfferExpiredEvent -> this.copy(
                status = TravelOfferStatusEnum.EXPIRED
            )
            is TravelOfferReservedEvent -> this.copy(
                status = TravelOfferStatusEnum.RESERVED,
                bookingId = event.bookingId
            )
            is TravelOfferBookedEvent -> this.copy(
                status = TravelOfferStatusEnum.BOOKED,
                bookingId = event.bookingId
            )
            is TravelOfferReservationCanceledEvent -> this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null
            )
            is TravelOfferReleaseEvent -> this.copy(
                status = TravelOfferStatusEnum.RELEASING
            )
            is TravelOfferRebookedEvent -> this.copy(
                status = TravelOfferStatusEnum.BOOKED
            )
            is TravelOfferBookingCanceledEvent -> this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null
            )
            // Failed events don't change the state
            is TravelOfferBookFailedEvent,
            is TravelOfferReserveFailedEvent,
            is TravelOfferMakeUnavailableFailedEvent,
            is TravelOfferMakeAvailableFailedEvent,
            is TravelOfferExpireFailedEvent,
            is TravelOfferReservationCancelFailedEvent,
            is TravelOfferBookingCancelFailedEvent,
            is TravelOfferReleaseCompleteFailedEvent,
            is TravelOfferRebookCompleteFailedEvent -> this
            else -> this
        }
    }
    companion object {
        fun create(
            name: String,
            commuteId: UUID,
            accommodationId: UUID,
            attractionId: UUID? = null,
        ): Pair<TravelOffer, TravelOfferCreatedEvent> {
            val travelOffer =
                TravelOffer(
                    _id = UUID.randomUUID(),
                    name = name,
                    commuteId = commuteId,
                    accommodationId = accommodationId,
                    attractionId = attractionId,
                )

            val event =
                TravelOfferCreatedEvent(
                    travelOfferId = travelOffer._id,
                    name = name,
                    commuteId = commuteId,
                    accommodationId = accommodationId,
                    attractionId = attractionId,
                )

            return travelOffer to event
        }
        
        fun fromEvents(events: List<TravelOfferEvent>): TravelOffer? {
            if (events.isEmpty()) return null
            
            // Find the first created event
            val createdEvent = events.find { it is TravelOfferCreatedEvent } as? TravelOfferCreatedEvent
                ?: return null
                
            // Create an initial state from the created event
            var travelOffer = TravelOffer(
                _id = createdEvent.travelOfferId,
                name = createdEvent.name,
                commuteId = createdEvent.commuteId,
                accommodationId = createdEvent.accommodationId,
                attractionId = createdEvent.attractionId,
            )
            
            // Apply all events in order to reconstruct the current state
            for (event in events) {
                travelOffer = travelOffer.apply(event)
            }
            
            return travelOffer
        }
    }

    fun makeUnavailable(): TravelOfferEvent {
        if (this.status != TravelOfferStatusEnum.AVAILABLE)
            return TravelOfferMakeUnavailableFailedEvent(
                travelOfferId = _id,
                message = "TravelOffer $_id cannot be made unavailable when in $status status"
            )

        this.status = TravelOfferStatusEnum.UNAVAILABLE

        return TravelOfferMadeUnavailableEvent(
            travelOfferId = _id,
        )
    }

    fun makeAvailable(): TravelOfferEvent {
        if (this.status != TravelOfferStatusEnum.UNAVAILABLE) {
            return TravelOfferMakeAvailableFailedEvent(
                travelOfferId = _id,
                message = "TravelOffer $_id cannot be made available when in $status status"
            )
        }

        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferMadeAvailableEvent(
            travelOfferId = _id,
        )
    }

    fun expire(): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.AVAILABLE) {
            return TravelOfferExpireFailedEvent(
                travelOfferId = _id,
                message = "TravelOffer $_id cannot be expired when in $status status"
            )
        }

        this.status = TravelOfferStatusEnum.EXPIRED

        return TravelOfferExpiredEvent(
            travelOfferId = _id,
            commuteId = commuteId,
            accommodationId = accommodationId,
            attractionId = attractionId,
        )
    }

    fun reserve(
        bookingId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.AVAILABLE)
            return TravelOfferReserveFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "TravelOffer is not open for reservation, current status is $status"
            )

        this.status = TravelOfferStatusEnum.RESERVED
        this.bookingId = bookingId

        return TravelOfferReservedEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    fun book(
        bookingId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.RESERVED)
            return TravelOfferBookFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "TravelOffer can not be booked if not RESERVED prior, current status is $status"
            )

        if (this.bookingId != bookingId)
            return TravelOfferBookFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "TravelOffer $_id is not RESERVED by booking $bookingId"
            )

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferBookedEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    fun cancelReservation(
        bookingId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.RESERVED) {
            return TravelOfferReservationCancelFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "Cannot cancel reservation for TravelOffer $_id when in $status status"
            )
        }

        if (this.bookingId != bookingId) {
            return TravelOfferReservationCancelFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "TravelOffer $_id is not RESERVED by user $bookingId"
            )
        }

        this.bookingId = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferReservationCanceledEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat
        )
    }

    fun releaseBooking(
        bookingId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.BOOKED) {
            return TravelOfferBookingCancelFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "Cannot cancel booking for TravelOffer $_id when in $status status"
            )
        }

        if (this.bookingId != bookingId) {
            return TravelOfferBookingCancelFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "TravelOffer $_id is not BOOKED for Booking $bookingId"
            )
        }

        this.status = TravelOfferStatusEnum.RELEASING

        return TravelOfferReleaseEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat
        )
    }

    fun rebook(
        bookingId: UUID,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.RELEASING) {
            return TravelOfferRebookCompleteFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "Cannot rebook TravelOffer $_id when in $status status",
            )
        }

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferRebookedEvent(
            travelOfferId = _id,
            bookingId = bookingId,
        )
    }

    fun cancelBooking(
        bookingId: UUID,
        seat: Seat
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.RELEASING) {
            return TravelOfferReleaseCompleteFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "Cannot cancel Booking for TravelOffer $_id when in $status status"
            )
        }

        if (this.bookingId != bookingId) {
            return TravelOfferReleaseCompleteFailedEvent(
                travelOfferId = _id,
                bookingId = bookingId,
                message = "TravelOffer $_id is not being released by Booking $bookingId"
            )
        }

        this.bookingId = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferBookingCanceledEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat
        )
    }
}
