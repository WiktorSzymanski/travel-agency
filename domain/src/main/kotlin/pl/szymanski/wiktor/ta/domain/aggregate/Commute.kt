package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import java.time.LocalDateTime
import java.util.UUID

data class Commute(
    val _id: UUID = UUID.randomUUID(),
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
    val bookings: MutableMap<String, Booking> = mutableMapOf(),
    var status: CommuteStatusEnum = CommuteStatusEnum.SCHEDULED,
) {
    companion object {
        const val MINIMUM_REQUIRED_BOOKINGS_RATIO = 0.5
    }

    fun cancel() {
        require(LocalDateTime.now().isAfter(this.departure.time)) {
            "Commute $_id cannot be canceled before its departure time"
        }

        require(status == CommuteStatusEnum.SCHEDULED) {
            "Commute $_id cannot be cancelled when not in SCHEDULED status"
        }

        require(bookings.size < MINIMUM_REQUIRED_BOOKINGS_RATIO * seats.size) {
            "Commute $_id cannot be cancelled when more than half of seats are booked"
        }

        this.status = CommuteStatusEnum.CANCELLED

        // EVENT or something
    }

    fun depart() {
        require(LocalDateTime.now().isAfter(this.departure.time)) {
            "Commute $_id cannot depart before its departure time"
        }
        this.status = CommuteStatusEnum.DEPARTED

        // EVENT or something
    }

    fun bookSeat(
        seat: Seat,
        userId: UUID,
    ): CommuteEvent {
        statusCheck()
        require(this.status == CommuteStatusEnum.SCHEDULED) {
            "Seat cannot be booked when Commute $_id not in SCHEDULED status"
        }

        require(this.seats.contains(seat)) {
            "Seat $seat not found in Commute $_id"
        }

        require(!this.bookings.containsKey(seat.toString())) {
            "Seat $seat already booked in Commute $_id"
        }

        this.bookings.put(seat.toString(), Booking(userId, LocalDateTime.now()))

        return CommuteBookedEvent(
            commuteId = _id,
            userId = userId,
            seat = seat,
        )
    }

    fun cancelBookedSeat(
        seat: Seat,
        userId: UUID,
    ): CommuteEvent {
        statusCheck()
        require(this.status == CommuteStatusEnum.SCHEDULED) {
            "Cannot cancel seat $seat when Commute $_id not in SCHEDULED status"
        }

        this.bookings
            .getOrElse(seat.toString(), {
                throw IllegalArgumentException("Booking for seat $seat not found in Commute $_id")
            })
            .let {
                require(it.userId == userId) {
                    "Booking for seat $seat in Commute $_id is owned by other user"
                }
                this.bookings.remove(seat.toString())
            }

        return CommuteBookingCanceledEvent(
            commuteId = _id,
            userId = userId,
            seat = seat,
        )
    }

    private fun statusCheck() {
        if (this.status == CommuteStatusEnum.SCHEDULED) {
            if (LocalDateTime.now().isAfter(this.departure.time)) {
                if (bookings.size < MINIMUM_REQUIRED_BOOKINGS_RATIO * seats.size) {
                    this.status = CommuteStatusEnum.CANCELLED
                } else {
                    this.status = CommuteStatusEnum.DEPARTED
                }
            }
        }
    }
}
