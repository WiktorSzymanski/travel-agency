package pl.szymanski.wiktor.ta.domain.exception

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId

open class CommuteException(message: String) : DomainException(message)

class CommuteBookSeatFailedException : CommuteException {
    companion object {
        fun seatAlreadyBooked(
            seat: Seat,
            commuteId: CommuteId,
        ): CommuteBookSeatFailedException = CommuteBookSeatFailedException("Seat $seat already booked in Commute $commuteId")
    }

    constructor(
        commuteId: CommuteId,
        status: CommuteStatusEnum,
    ) : super("Seat cannot be booked when Commute $commuteId not in SCHEDULED status, current status is $status")
    constructor(commuteId: CommuteId) : super("No available seats in Commute $commuteId")
    constructor(seat: Seat, commuteId: CommuteId) : super("Seat $seat not found in Commute $commuteId")
    private constructor(message: String) : super(message)
}

class CommuteCancelBookedSeatFailedException : CommuteException {
    constructor(bookingId: BookingId, commuteId: CommuteId) : super("No seat assigned for booking $bookingId in Commute $commuteId")
    constructor(
        bookingId: BookingId,
        commuteId: CommuteId,
        status: CommuteStatusEnum,
    ) : super(
        "Cannot cancel seat booking for booking $bookingId when Commute $commuteId not in SCHEDULED status, current status is $status",
    )
}

class CommuteExpireFailedException : CommuteException {
    constructor(commuteId: CommuteId) : super("Commute $commuteId cannot expire before its departure time")
    constructor(commuteId: CommuteId, status: CommuteStatusEnum) : super("Commute $commuteId cannot expire when not in $status status")
}

class CommuteMissingCreatedEventException : CommuteException("First event must be CommuteCreatedEvent")

class CommuteEmptyEventListException : CommuteException("Commute events list cannot be empty")