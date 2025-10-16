package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import java.util.UUID

open class DomainException(message: String) : RuntimeException(message)

// Accommodation
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

// Commute
open class CommuteException(message: String) : DomainException(message)

class CommuteBookSeatFailedException : CommuteException {
    constructor(
        commuteId: UUID,
        status: CommuteStatusEnum,
    ) : super("Seat cannot be booked when Commute $commuteId not in SCHEDULED status, current status is $status")
    constructor(commuteId: UUID) : super("No available seats in Commute $commuteId")
    constructor(seat: Seat, commuteId: UUID) : super("Seat $seat not found in Commute $commuteId")
    constructor(seat: Seat, commuteId: UUID, alreadyBooked: Boolean) : super("Seat $seat already booked in Commute $commuteId")
}

class CommuteCancelBookedSeatFailedException : CommuteException {
    constructor(bookingId: UUID, commuteId: UUID) : super("No seat assigned for booking $bookingId in Commute $commuteId")
    constructor(
        bookingId: UUID,
        commuteId: UUID,
        status: CommuteStatusEnum,
    ) : super(
        "Cannot cancel seat booking for booking $bookingId when Commute $commuteId not in SCHEDULED status, current status is $status",
    )
}

class CommuteExpireFailedException : CommuteException {
    constructor(commuteId: UUID) : super("Commute $commuteId cannot expire before its departure time")
    constructor(commuteId: UUID, status: CommuteStatusEnum) : super("Commute $commuteId cannot expire when not in $status status")
}

// Attraction
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

// TravelOffer
open class TravelOfferException(message: String) : DomainException(message)

class TravelOfferReserveFailedException(status: TravelOfferStatusEnum) : TravelOfferException(
    "TravelOffer is not open for reservation, current status is $status",
)

class TravelOfferBookFailedException : TravelOfferException {
    constructor(status: TravelOfferStatusEnum) : super("TravelOffer can not be booked if not RESERVED prior, current status is $status")
    constructor(travelOfferId: UUID, bookingId: UUID) : super("TravelOffer $travelOfferId is not RESERVED by booking $bookingId")
}

class TravelOfferExpireFailedException(travelOfferId: UUID, status: TravelOfferStatusEnum) : TravelOfferException(
    "TravelOffer $travelOfferId cannot be expired when in $status status",
)

class TravelOfferBookingCancelFailedException : TravelOfferException {
    constructor(
        travelOfferId: UUID,
        status: TravelOfferStatusEnum,
    ) : super("Cannot cancel booking for TravelOffer $travelOfferId when in $status status")
    constructor(travelOfferId: UUID, bookingId: UUID) : super("TravelOffer $travelOfferId is not BOOKED for Booking $bookingId")
}

class TravelOfferReleaseCompleteFailedException : TravelOfferException {
    constructor(
        travelOfferId: UUID,
        status: TravelOfferStatusEnum,
    ) : super("Cannot complete release for TravelOffer $travelOfferId when in $status status")
    constructor(travelOfferId: UUID, bookingId: UUID) : super("TravelOffer $travelOfferId is not being released by Booking $bookingId")
}

class TravelOfferMakeAvailableFailedException(travelOfferId: UUID, status: TravelOfferStatusEnum) : TravelOfferException(
    "TravelOffer $travelOfferId cannot be made available when in $status status",
)

class TravelOfferMakeUnavailableFailedException(travelOfferId: UUID, status: TravelOfferStatusEnum) : TravelOfferException(
    "TravelOffer $travelOfferId cannot be made unavailable when in $status status",
)

class TravelOfferReservationCancelFailedException : TravelOfferException {
    constructor(
        travelOfferId: UUID,
        status: TravelOfferStatusEnum,
    ) : super("Cannot cancel reservation for TravelOffer $travelOfferId when in $status status")
    constructor(travelOfferId: UUID, bookingId: UUID) : super("TravelOffer $travelOfferId is not RESERVED by user $bookingId")
}

class TravelOfferRebookFailedException(travelOfferId: UUID, status: TravelOfferStatusEnum) : TravelOfferException(
    "Cannot rebook TravelOffer $travelOfferId when in $status status",
)

// Booking
open class BookingException(message: String) : DomainException(message)

class BookingProcessFailedException : BookingException("Booking can only be processed in NEW state")

class BookingCompleteFailedException : BookingException("Booking can only be completed in PROCESSING state")

class BookingCancelRequestFailedException : BookingException("Booking can only be cancelled in SUCCEEDED state")

class BookingCancelFailedException : BookingException("Booking can only be cancelled in PROCESSING or NEW state")

class BookingProcessCancellationFailedException : BookingException("Booking cancelation can only be process in CANCEL_REQUESTED state")

class BookingFailFailedException(originalMessage: String) : BookingException(
    "Booking can only be failed in PROCESSING or NEW state. Original message: $originalMessage",
)

class BookingFailCancellationFailedException(originalMessage: String) : BookingException(
    "Cancel Booking can only be failed in PROCESSING_CANCELLATION or CANCEL_REQUESTED state. Original message: $originalMessage",
)
