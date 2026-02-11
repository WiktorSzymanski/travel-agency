package pl.szymanski.wiktor.ta.domain.aggregate

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.exception.CommuteBookSeatFailedException
import pl.szymanski.wiktor.ta.domain.exception.CommuteCancelBookedSeatFailedException
import pl.szymanski.wiktor.ta.domain.exception.CommuteExpireFailedException
import pl.szymanski.wiktor.ta.domain.exception.CommuteMissingCreatedEventException
import pl.szymanski.wiktor.ta.domain.exception.CommuteEmptyEventListException
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Commute(
    val id: CommuteId = CommuteId.generate(),
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
    val bookings: MutableMap<BookingId, Seat> = mutableMapOf(),
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

        fun fromEvents(events: List<CommuteEvent>): Commute {
            if (events.isEmpty())
                throw CommuteEmptyEventListException()

            val createdEvent = events.first()
            if (createdEvent !is CommuteCreatedEvent)
                throw CommuteMissingCreatedEventException()

            val commute =
                Commute(
                    id = createdEvent.commuteId,
                    name = createdEvent.name,
                    departure = createdEvent.departure,
                    arrival = createdEvent.arrival,
                    seats = createdEvent.seats,
                )

            for (event in events.drop(1)) {
                commute.apply(event)
            }

            return commute
        }
    }

    fun apply(event: CommuteEvent): Unit = when (event) {
        is CommuteCreatedEvent -> Unit

        is CommuteBookedEvent -> {
            this.bookings[event.bookingId] = event.seat
        }

        is CommuteFullEvent -> {
            this.status = CommuteStatusEnum.FULL
        }

        is CommuteAvailableEvent -> {
            this.status = CommuteStatusEnum.SCHEDULED
        }

        is CommuteBookingCanceledEvent -> {
            this.bookings.remove(event.bookingId)
            Unit
        }

        is CommuteExpiredEvent -> {
            this.status = CommuteStatusEnum.EXPIRED
        }

        is CommuteBookedCompensatedEvent -> {
            this.bookings.remove(event.bookingId)
            Unit
        }

        is CommuteBookingCanceledCompensatedEvent -> {
            this.bookings[event.bookingId] = event.seat
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
        bookingId: BookingId,
        seat: Seat,
    ): List<CommuteEvent> {
        statusCheck()
        if (this.status != CommuteStatusEnum.SCHEDULED) {
            throw CommuteBookSeatFailedException(id, status)
        }

        val seatToBook = when (seat) {
            is Seat.Any -> getFirstAvailableSeat()
            is Seat.Picked -> validateSeat(seat)
        }

        this.bookings[bookingId] = seatToBook

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

    fun cancelBookedSeat(bookingId: BookingId): List<CommuteEvent> {
        statusCheck()
        if (!listOf(CommuteStatusEnum.SCHEDULED, CommuteStatusEnum.FULL).contains(this.status)) {
            throw CommuteCancelBookedSeatFailedException(bookingId, id, status)
        }

        val seat =
            this.bookings.remove(bookingId)
                ?: throw CommuteCancelBookedSeatFailedException(bookingId, id)

        return listOfNotNull(
            CommuteBookingCanceledEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = seat,
            ),
            takeIf { seatsCheck() }?.let {
                CommuteAvailableEvent(
                    commuteId = id,
                )
            },
        )
    }

    fun compensateCancelBookedSeat(
        bookingId: BookingId,
        seat: Seat,
    ): List<CommuteEvent> {
        if (!this.seats.contains(seat)) {
            throw CommuteBookSeatFailedException(seat, id)
        }

        if (this.bookings.containsValue(seat)) {
            throw CommuteBookSeatFailedException.seatAlreadyBooked(seat, id)
        }

        this.bookings[bookingId] = seat

        return listOfNotNull(
            CommuteBookingCanceledCompensatedEvent(
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

    fun compensateBookSeat(bookingId: BookingId): List<CommuteEvent> {
        val seat =
            this.bookings.remove(bookingId)
                ?: throw CommuteCancelBookedSeatFailedException(bookingId, id)

        return listOfNotNull(
            CommuteBookedCompensatedEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = seat,
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

    private fun getFirstAvailableSeat(): Seat {
        val availableSeats = this.seats.filter { !this.bookings.containsValue(it) }
        if (availableSeats.isEmpty()) {
            throw CommuteBookSeatFailedException(id)
        }
        return availableSeats.first()
    }

    private fun validateSeat(seat: Seat): Seat {
        if (!this.seats.contains(seat))
            throw CommuteBookSeatFailedException(seat, id)

        if (this.bookings.containsValue(seat))
            throw CommuteBookSeatFailedException.seatAlreadyBooked(seat, id)

        return seat
    }
}
