package pl.szymanski.wiktor.ta.domain.exception

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId

open class AccommodationException(message: String) : DomainException(message)

class AccommodationBookingFailedException : AccommodationException {
    constructor(
        accommodationId: AccommodationId,
        status: AccommodationStatusEnum,
    ) : super("Accommodation $accommodationId cannot be booked when in status $status")
    constructor(accommodationId: AccommodationId) : super("Accommodation $accommodationId is already booked")
}

class AccommodationBookingCancelFailedException : AccommodationException {
    constructor(
        accommodationId: AccommodationId,
        status: AccommodationStatusEnum,
    ) : super("Accommodation $accommodationId booking cannot be canceled when in status $status")
    constructor(accommodationId: AccommodationId, bookingId: BookingId) : super("Accommodation $accommodationId is not BOOKED by bookingId $bookingId")
}

class AccommodationExpireFailedException : AccommodationException {
    constructor(
        accommodationId: AccommodationId,
        status: AccommodationStatusEnum,
    ) : super("Accommodation $accommodationId cannot expire in status $status")
    constructor(accommodationId: AccommodationId) : super("Accommodation $accommodationId cannot be expired before its rent start")
}

class AccommodationMissingCreatedEventException : AccommodationException("First event must be AccommodationCreatedEvent")

class AccommodationEmptyEventListException : AccommodationException("Accommodation events list cannot be empty")
