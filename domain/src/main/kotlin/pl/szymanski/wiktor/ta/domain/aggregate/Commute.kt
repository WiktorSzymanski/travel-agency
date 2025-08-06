package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import java.time.LocalDateTime
import java.util.UUID

data class Commute(
    val _id: UUID = UUID.randomUUID(),
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
    val bookings: MutableMap<String, String> = mutableMapOf(),
    var status: CommuteStatusEnum = CommuteStatusEnum.SCHEDULED,
    val version: Int = 1,
) {
    companion object {
        fun create(
            name: String,
            departure: LocationAndTime,
            arrival: LocationAndTime,
            seats: List<Seat>,
        ): Pair<Commute, CommuteCreatedEvent> {
            val commute =
                Commute(
                    name = name,
                    departure = departure,
                    arrival = arrival,
                    seats = seats,
                )

            val event =
                CommuteCreatedEvent(
                    commuteId = commute._id,
                    name = name,
                    departure = departure,
                    arrival = arrival,
                    seats = seats,
                )

            return commute to event
        }
    }

    fun expire(): CommuteEvent {
        require(LocalDateTime.now().isAfter(this.departure.time)) {
            "Commute $_id cannot expire before its departure time"
        }

        require(listOf(CommuteStatusEnum.SCHEDULED, CommuteStatusEnum.FULL).contains(this.status)) {
            "Commute $_id cannot expire when not in SCHEDULED or FULL status"
        }

        this.status = CommuteStatusEnum.EXPIRED

        return CommuteExpiredEvent(
            commuteId = _id,
        )
    }

    fun bookSeat(
        bookingId: UUID,
        seat: Seat
    ): CommuteEvent {
        statusCheck()
        require(this.status == CommuteStatusEnum.SCHEDULED) {
            "Seat cannot be booked when Commute $_id not in SCHEDULED status, current status is $status"
        }

        require(this.seats.contains(seat)) {
            "Seat $seat not found in Commute $_id"
        }

        require(!this.bookings.containsValue(seat.toString())) {
            "Seat $seat already booked in Commute $_id"
        }

        this.bookings.put(bookingId.toString(), seat.toString())

        return CommuteBookedEvent(
            commuteId = _id,
            bookingId = bookingId,
            seat = seat
        )
    }

    fun cancelBookedSeat(
        bookingId: UUID,
    ): CommuteEvent {
        statusCheck()
        require(this.status == CommuteStatusEnum.SCHEDULED) {
            "Cannot cancel seat booking for booking $bookingId when Commute $_id not in SCHEDULED status, current status is $status"
        }

        val seat = this.bookings.remove(bookingId.toString())
            ?: throw IllegalArgumentException("No seat assigned for booking $bookingId in Commute $_id")

        return CommuteBookingCanceledEvent(
            commuteId = _id,
            bookingId = bookingId,
            seat = Seat.fromString(seat)
        )
    }

    private fun statusCheck() {
        if (!listOf(CommuteStatusEnum.SCHEDULED, CommuteStatusEnum.FULL).contains(this.status)) return
        if (LocalDateTime.now().isBefore(this.departure.time)) return

        this.status = CommuteStatusEnum.EXPIRED
    }
}
