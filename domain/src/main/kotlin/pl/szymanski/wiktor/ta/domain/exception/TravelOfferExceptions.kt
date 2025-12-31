package pl.szymanski.wiktor.ta.domain.exception

import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId

open class TravelOfferException(message: String) : DomainException(message)

class TravelOfferReserveFailedException(status: TravelOfferStatusEnum) : TravelOfferException(
    "TravelOffer is not open for reservation, current status is $status",
)

class TravelOfferBookFailedException : TravelOfferException {
    constructor(status: TravelOfferStatusEnum) : super("TravelOffer can not be booked if not RESERVED prior, current status is $status")
    constructor(travelOfferId: TravelOfferId, bookingId: BookingId) : super("TravelOffer $travelOfferId is not RESERVED by booking $bookingId")
}

class TravelOfferExpireFailedException(travelOfferId: TravelOfferId, status: TravelOfferStatusEnum) : TravelOfferException(
    "TravelOffer $travelOfferId cannot be expired when in $status status",
)

class TravelOfferBookingCancelFailedException : TravelOfferException {
    constructor(
        travelOfferId: TravelOfferId,
        status: TravelOfferStatusEnum,
    ) : super("Cannot cancel booking for TravelOffer $travelOfferId when in $status status")
    constructor(travelOfferId: TravelOfferId, bookingId: BookingId) : super("TravelOffer $travelOfferId is not BOOKED for Booking $bookingId")
}

class TravelOfferReleaseCompleteFailedException : TravelOfferException {
    constructor(
        travelOfferId: TravelOfferId,
        status: TravelOfferStatusEnum,
    ) : super("Cannot complete release for TravelOffer $travelOfferId when in $status status")
    constructor(travelOfferId: TravelOfferId, bookingId: BookingId) : super("TravelOffer $travelOfferId is not being released by Booking $bookingId")
}

class TravelOfferMakeAvailableFailedException(travelOfferId: TravelOfferId, status: TravelOfferStatusEnum) : TravelOfferException(
    "TravelOffer $travelOfferId cannot be made available when in $status status",
)

class TravelOfferMakeUnavailableFailedException(travelOfferId: TravelOfferId, status: TravelOfferStatusEnum) : TravelOfferException(
    "TravelOffer $travelOfferId cannot be made unavailable when in $status status",
)

class TravelOfferReservationCancelFailedException : TravelOfferException {
    constructor(
        travelOfferId: TravelOfferId,
        status: TravelOfferStatusEnum,
    ) : super("Cannot cancel reservation for TravelOffer $travelOfferId when in $status status")
    constructor(travelOfferId: TravelOfferId, bookingId: BookingId) : super("TravelOffer $travelOfferId is not RESERVED by user $bookingId")
}

class TravelOfferRebookFailedException(travelOfferId: TravelOfferId, status: TravelOfferStatusEnum) : TravelOfferException(
    "Cannot rebook TravelOffer $travelOfferId when in $status status",
)

class TravelOfferMissingCreatedEventException : TravelOfferException("First event must be TravelOfferCreatedEvent")

class TravelOfferEmptyEventListException : TravelOfferException("Travel offer events list cannot be empty")