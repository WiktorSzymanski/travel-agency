package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import java.util.UUID

data class TravelOffer(
    val id: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
    var bookingId: UUID? = null,
    var status: TravelOfferStatusEnum = TravelOfferStatusEnum.AVAILABLE,
    val lastRevision: Int = -1,
) {
    fun makeUnavailable(): TravelOfferEvent {
        if (this.status != TravelOfferStatusEnum.AVAILABLE) {
            throw TravelOfferMakeUnavailableFailedException(id, status)
        }

        this.status = TravelOfferStatusEnum.UNAVAILABLE

        return TravelOfferMadeUnavailableEvent(
            travelOfferId = id,
        )
    }

    fun makeAvailable(): TravelOfferEvent {
        if (this.status != TravelOfferStatusEnum.UNAVAILABLE) {
            throw TravelOfferMakeAvailableFailedException(id, status)
        }

        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferMadeAvailableEvent(
            travelOfferId = id,
        )
    }

    fun expire(): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.AVAILABLE) {
            throw TravelOfferExpireFailedException(id, status)
        }

        this.status = TravelOfferStatusEnum.EXPIRED

        return TravelOfferExpiredEvent(
            travelOfferId = id,
            commuteId = commuteId,
            accommodationId = accommodationId,
            attractionId = attractionId,
        )
    }

    fun reserve(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.AVAILABLE) {
            throw TravelOfferReserveFailedException(status)
        }

        this.status = TravelOfferStatusEnum.RESERVED
        this.bookingId = bookingId

        return TravelOfferReservedEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    fun book(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.RESERVED) {
            throw TravelOfferBookFailedException(status)
        }

        if (this.bookingId != bookingId) {
            throw TravelOfferBookFailedException(id, bookingId)
        }

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferBookedEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    fun cancelReservation(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.RESERVED) {
            throw TravelOfferReservationCancelFailedException(id, status)
        }

        if (this.bookingId != bookingId) {
            throw TravelOfferReservationCancelFailedException(id, bookingId)
        }

        this.bookingId = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferReservationCanceledEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    fun releaseBooking(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.BOOKED) {
            throw TravelOfferBookingCancelFailedException(id, status)
        }

        if (this.bookingId != bookingId) {
            throw TravelOfferBookingCancelFailedException(id, bookingId)
        }

        this.status = TravelOfferStatusEnum.RELEASING

        return TravelOfferReleaseEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    fun rebook(bookingId: UUID): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.RELEASING) {
            throw TravelOfferRebookFailedException(id, status)
        }

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferRebookedEvent(
            travelOfferId = id,
            bookingId = bookingId,
        )
    }

    fun cancelBooking(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        if (status != TravelOfferStatusEnum.RELEASING) {
            throw TravelOfferReleaseCompleteFailedException(id, status)
        }

        if (this.bookingId != bookingId) {
            throw TravelOfferReleaseCompleteFailedException(id, bookingId)
        }

        this.bookingId = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferBookingCanceledEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }
}
