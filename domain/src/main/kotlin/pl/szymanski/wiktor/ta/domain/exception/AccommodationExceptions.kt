package pl.szymanski.wiktor.ta.domain.exception

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import java.util.UUID

open class AccommodationException(message: String) : DomainException(message)

class AccommodationBookingFailedException : AccommodationException {
    constructor(
        accommodationId: UUID,
        status: AccommodationStatusEnum,
    ) : super("Accommodation $accommodationId cannot be booked when in status $status")
    constructor(accommodationId: UUID) : super("Accommodation $accommodationId is already booked")
}

class AccommodationBookingCancelFailedException : AccommodationException {
    constructor(
        accommodationId: UUID,
        status: AccommodationStatusEnum,
    ) : super("Accommodation $accommodationId booking cannot be canceled when in status $status")
    constructor(accommodationId: UUID, bookingId: UUID) : super("Accommodation $accommodationId is not BOOKED by bookingId $bookingId")
}

class AccommodationExpireFailedException : AccommodationException {
    constructor(
        accommodationId: UUID,
        status: AccommodationStatusEnum,
    ) : super("Accommodation $accommodationId cannot expire in status $status")
    constructor(accommodationId: UUID) : super("Accommodation $accommodationId cannot be expired before its rent start")
}

class AccommodationMissingCreatedEventException : AccommodationException("First event must be AccommodationCreatedEvent")

class AccommodationEmptyEventListException : AccommodationException("Accommodation events list cannot be empty")
