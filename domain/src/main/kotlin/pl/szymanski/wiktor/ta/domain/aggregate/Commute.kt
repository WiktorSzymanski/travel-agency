package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import java.time.LocalDateTime
import java.util.UUID

data class Commute(
    val commuteId: UUID = UUID.randomUUID(),
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
        require(status == CommuteStatusEnum.SCHEDULED) {
            "Commute $commuteId cannot be cancelled when not in SCHEDULED status"
        }

        require(bookings.size < MINIMUM_REQUIRED_BOOKINGS_RATIO * seats.size) {
            "Commute $commuteId cannot be cancelled when more than half of seats are booked"
        }

        this.status = CommuteStatusEnum.CANCELLED

        // EVENT or something
    }

    fun depart() {
        require(LocalDateTime.now().isAfter(this.departure.time)) {
            "Commute $commuteId cannot depart before its departure time"
        }
        this.status = CommuteStatusEnum.DEPARTED

        // EVENT or something
    }

    fun bookSeat(
        seat: Seat,
        userId: UUID,
    ) {
        statusCheck()
        require(this.status == CommuteStatusEnum.SCHEDULED) {
            "Seat cannot be booked when Commute $commuteId not in SCHEDULED status"
        }

        require(this.seats.contains(seat)) {
            "Seat $seat not found in Commute $commuteId"
        }

        require(!this.bookings.containsKey(seat.toString())) {
            "Seat $seat already booked in Commute $commuteId"
        }

        this.bookings.put(seat.toString(), Booking(userId, LocalDateTime.now()))

        // EVENT or something
    }

    fun cancelBookedSeat(
        seat: Seat,
        userId: UUID,
    ) {
        statusCheck()
        require(this.status == CommuteStatusEnum.SCHEDULED) {
            "Cannot cancel seat $seat when Commute $commuteId not in SCHEDULED status"
        }

        this.bookings
            .getOrElse(seat.toString(), {
                throw IllegalArgumentException("Booking for seat $seat not found in Commute $commuteId")
            })
            .let {
                require(it.userId == userId) {
                    "Booking for seat $seat in Commute $commuteId is owned by other user"
                }
                this.bookings.remove(seat.toString())
            }

        // EVENT or something
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
