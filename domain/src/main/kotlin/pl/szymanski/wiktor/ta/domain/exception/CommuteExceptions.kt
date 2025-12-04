package pl.szymanski.wiktor.ta.domain.exception

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

open class CommuteException(message: String) : DomainException(message)

class CommuteBookSeatFailedException : CommuteException {
    companion object {
        fun seatAlreadyBooked(
            seat: Seat,
            commuteId: UUID,
        ): CommuteBookSeatFailedException = CommuteBookSeatFailedException("Seat $seat already booked in Commute $commuteId")
    }

    constructor(
        commuteId: UUID,
        status: CommuteStatusEnum,
    ) : super("Seat cannot be booked when Commute $commuteId not in SCHEDULED status, current status is $status")
    constructor(commuteId: UUID) : super("No available seats in Commute $commuteId")
    constructor(seat: Seat, commuteId: UUID) : super("Seat $seat not found in Commute $commuteId")
    private constructor(message: String) : super(message)
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
