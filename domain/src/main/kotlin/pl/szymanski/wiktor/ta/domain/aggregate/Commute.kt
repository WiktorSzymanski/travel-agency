package pl.szymanski.wiktor.ta.domain.aggregate

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.AnySeat
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.PickedSeat
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
            id: CommuteId,
            name: String,
            departure: LocationAndTime,
            arrival: LocationAndTime,
            seats: List<Seat>,
        ): Pair<Commute, CommuteCreatedEvent> {
            val commute =
                Commute(
                    id = id,
                    name = name,
                    departure = departure,
                    arrival = arrival,
                    seats = seats,
                )

            val event =
                CommuteCreatedEvent(
                    commuteId = commute.id.value,
                    name = name,
                    departure = departure,
                    arrival = arrival,
                    seats = seats,
                )

            return commute to event
        }

        fun fromEvents(events: List<CommuteEvent>): Commute {
            if (events.isEmpty())
                throw CommuteEmptyEventListException()

            val createdEvent = events.first()
            if (createdEvent !is CommuteCreatedEvent)
                throw CommuteMissingCreatedEventException()

            val commute =
                Commute(
                    id = CommuteId.from(createdEvent.commuteId),
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
            this.bookings[BookingId.from(event.bookingId)] = event.seat
        }

        is CommuteFullEvent -> {
            this.status = CommuteStatusEnum.FULL
        }

        is CommuteAvailableEvent -> {
            this.status = CommuteStatusEnum.SCHEDULED
        }

        is CommuteBookingCanceledEvent -> {
            this.bookings.remove(BookingId.from(event.bookingId))
            Unit
        }

        is CommuteExpiredEvent -> {
            this.status = CommuteStatusEnum.EXPIRED
        }

        is CommuteBookedCompensatedEvent -> {
            this.bookings.remove(BookingId.from(event.bookingId))
            Unit
        }

        is CommuteBookingCanceledCompensatedEvent -> {
            this.bookings[BookingId.from(event.bookingId)] = event.seat
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
                commuteId = id.value,
            ),
        )
    }

    fun bookSeat(
        bookingId: BookingId,
        seat: Seat,
    ): List<CommuteEvent> {
        checkAvailability(seat)

        val seatToBook = when (seat) {
            is AnySeat -> getFirstAvailableSeat()
            is PickedSeat -> validateSeat(seat)
        }

        this.bookings[bookingId] = seatToBook

        return listOfNotNull(
            CommuteBookedEvent(
                commuteId = id.value,
                bookingId = bookingId.value!!,
                seat = seatToBook,
            ),
            takeIf { seatsCheck() }?.let {
                CommuteFullEvent(
                    commuteId = id.value,
                )
            },
        )
    }

    fun checkAvailability(seat: Seat) {
        statusCheck()
        if (this.status != CommuteStatusEnum.SCHEDULED) {
            throw CommuteBookSeatFailedException(id, status)
        }

        when (seat) {
            is AnySeat -> getFirstAvailableSeat()
            is PickedSeat -> validateSeat(seat)
        }
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
                commuteId = id.value,
                bookingId = bookingId.value!!,
                seat = seat,
            ),
            takeIf { seatsCheck() }?.let {
                CommuteAvailableEvent(
                    commuteId = id.value,
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
                commuteId = id.value,
                bookingId = bookingId.value!!,
                seat = seat,
            ),
            takeIf { seatsCheck() }?.let {
                CommuteFullEvent(
                    commuteId = id.value,
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
                commuteId = id.value,
                bookingId = bookingId.value!!,
                seat = seat,
            ),
            takeIf { seatsCheck() }?.let {
                CommuteAvailableEvent(
                    commuteId = id.value,
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
