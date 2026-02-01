package pl.szymanski.wiktor.ta.domain.exception

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId

open class AttractionException(message: String) : DomainException(message)

class AttractionBookFailedException : AttractionException {
    constructor(
        attractionId: AttractionId,
        status: AttractionStatusEnum,
    ) : super("Attraction $attractionId is not open for booking, current status is $status")
    constructor(bookingId: BookingId, attractionId: AttractionId) : super("Booking $bookingId already signed for Attraction $attractionId")
    constructor(attractionId: AttractionId) : super("Attraction $attractionId is fully booked")
}

class AttractionBookingCancelFailedException : AttractionException {
    constructor(
        attractionId: AttractionId,
        status: AttractionStatusEnum,
    ) : super("Cannot cancel booking when Attraction $attractionId in status $status")
    constructor(bookingId: BookingId, attractionId: AttractionId) : super("Booking $bookingId was not signed for Attraction $attractionId")
}

class AttractionExpireFailedException(attractionId: AttractionId) : AttractionException("Attraction $attractionId cannot expire before its date")

class AttractionMissingCreatedEventException : AttractionException("First event must be AttractionCreatedEvent")

class AttractionEmptyEventListException : AttractionException("Attraction events list cannot be empty")
