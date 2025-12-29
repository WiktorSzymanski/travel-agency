package pl.szymanski.wiktor.ta.domain.exception

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import java.util.UUID

open class AttractionException(message: String) : DomainException(message)

class AttractionBookFailedException : AttractionException {
    constructor(
        attractionId: UUID,
        status: AttractionStatusEnum,
    ) : super("Attraction $attractionId is not open for booking, current status is $status")
    constructor(bookingId: UUID, attractionId: UUID) : super("Booking $bookingId already signed for Attraction $attractionId")
    constructor(attractionId: UUID) : super("Attraction $attractionId is fully booked")
}

class AttractionBookingCancelFailedException : AttractionException {
    constructor(
        attractionId: UUID,
        status: AttractionStatusEnum,
    ) : super("Cannot cancel booking for Attraction $attractionId not in SCHEDULED or FULL status")
    constructor(bookingId: UUID, attractionId: UUID) : super("Booking $bookingId was not signed for Attraction $attractionId")
}

class AttractionExpireFailedException(attractionId: UUID) : AttractionException("Attraction $attractionId cannot expire before its date")

class AttractionMissingCreatedEventException : AttractionException("First event must be AttractionCreatedEvent")

class AttractionEmptyEventListException : AttractionException("Attraction events list cannot be empty")