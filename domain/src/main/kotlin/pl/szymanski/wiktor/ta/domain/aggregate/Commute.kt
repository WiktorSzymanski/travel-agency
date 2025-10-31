package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.exception.CommuteBookSeatFailedException
import pl.szymanski.wiktor.ta.domain.exception.CommuteCancelBookedSeatFailedException
import pl.szymanski.wiktor.ta.domain.exception.CommuteExpireFailedException
import java.time.LocalDateTime
import java.util.UUID

data class Commute(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
    val bookings: MutableMap<String, String> = mutableMapOf(),
    var status: CommuteStatusEnum = CommuteStatusEnum.SCHEDULED,
) {
    companion object {
        fun create(
            name: String,
            departure: LocationAndTime,
            arrival: LocationAndTime,
            seats: List<Seat>,
        ): Pair<Commute, List<CommuteCreatedEvent>> {
            val commute =
                Commute(
                    name = name,
                    departure = departure,
                    arrival = arrival,
                    seats = seats,
                )

            val event =
                CommuteCreatedEvent(
                    commuteId = commute.id,
                    name = name,
                    departure = departure,
                    arrival = arrival,
                    seats = seats,
                )

            return commute to listOf(event)
        }
    }

    fun expire(): List<CommuteEvent> {
        if (LocalDateTime.now().isBefore(this.departure.time)) {
            throw CommuteExpireFailedException(id)
        }

        if (this.status == CommuteStatusEnum.EXPIRED) {
            throw CommuteExpireFailedException(id, status)
        }

        this.status = CommuteStatusEnum.EXPIRED

        return listOf(
            CommuteExpiredEvent(
                commuteId = id,
            ),
        )
    }

    fun bookSeat(
        bookingId: UUID,
        seat: Seat? = null,
    ): List<CommuteEvent> {
        statusCheck()
        if (this.status != CommuteStatusEnum.SCHEDULED) {
            throw CommuteBookSeatFailedException(id, status)
        }

        val seatToBook =
            when (seat) {
                null -> {
                    val availableSeats = this.seats.filter { !this.bookings.containsValue(it.toString()) }
                    if (availableSeats.isEmpty()) {
                        throw CommuteBookSeatFailedException(id)
                    }
                    availableSeats[0]
                }
                is Seat -> {
                    if (!this.seats.contains(seat)) {
                        throw CommuteBookSeatFailedException(seat, id)
                    }

                    if (this.bookings.containsValue(seat.toString())) {
                        throw CommuteBookSeatFailedException(seat, id, true)
                    }

                    seat
                }
            }

        this.bookings[bookingId.toString()] = seatToBook.toString()

        return listOfNotNull(
            CommuteBookedEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = seatToBook,
            ),
            takeIf { seatsCheck() }?.let {
                CommuteFullEvent(
                    commuteId = id,
                )
            },
        )
    }

    fun cancelBookedSeat(bookingId: UUID): List<CommuteEvent> {
        statusCheck()
        if (!listOf(CommuteStatusEnum.SCHEDULED, CommuteStatusEnum.FULL).contains(this.status)) {
            throw CommuteCancelBookedSeatFailedException(bookingId, id, status)
        }

        val seatStr =
            this.bookings.remove(bookingId.toString())
                ?: throw CommuteCancelBookedSeatFailedException(bookingId, id)

        return listOfNotNull(
            CommuteBookingCanceledEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = Seat.fromString(seatStr),
            ),
            takeIf { seatsCheck() }?.let {
                CommuteAvailableEvent(
                    commuteId = id,
                )
            },
        )
    }

    fun compensateCancelBookedSeat(
        bookingId: UUID,
        seat: Seat,
    ): List<CommuteEvent> {
        if (!this.seats.contains(seat)) {
            throw CommuteBookSeatFailedException(seat, id)
        }

        if (this.bookings.containsValue(seat.toString())) {
            throw CommuteBookSeatFailedException(seat, id, true)
        }

        this.bookings[bookingId.toString()] = seat.toString()

        return listOfNotNull(
            CommuteBookedEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = seat,
            ),
            takeIf { seatsCheck() }?.let {
                CommuteFullEvent(
                    commuteId = id,
                )
            },
        )
    }

    fun compensateBookSeat(bookingId: UUID): List<CommuteEvent> {
        val seatStr =
            this.bookings.remove(bookingId.toString())
                ?: throw CommuteCancelBookedSeatFailedException(bookingId, id)

        return listOfNotNull(
            CommuteBookingCanceledEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = Seat.fromString(seatStr),
            ),
            takeIf { seatsCheck() }?.let {
                CommuteAvailableEvent(
                    commuteId = id,
                )
            },
        )
    }

    private fun seatsCheck(): Boolean {
        return when (this.seats.size == this.bookings.size) {
            true -> {
                this.status = CommuteStatusEnum.FULL
                true
            }
            false -> {
                if (this.status == CommuteStatusEnum.FULL) {
                    this.status = CommuteStatusEnum.SCHEDULED
                    return true
                }
                false
            }
        }
    }

    private fun statusCheck() {
        if (!listOf(CommuteStatusEnum.SCHEDULED, CommuteStatusEnum.FULL).contains(this.status)) return
        if (LocalDateTime.now().isBefore(this.departure.time)) return

        this.status = CommuteStatusEnum.EXPIRED
    }
}
