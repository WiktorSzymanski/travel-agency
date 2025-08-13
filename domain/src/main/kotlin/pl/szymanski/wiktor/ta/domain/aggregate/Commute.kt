package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookSeatFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCancelBookedSeatFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
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
    fun apply(event: CommuteEvent): Commute {
        return when (event) {
            is CommuteCreatedEvent -> this.copy(
                _id = event.commuteId,
                name = event.name,
                departure = event.departure,
                arrival = event.arrival,
                seats = event.seats,
                status = CommuteStatusEnum.SCHEDULED,
                bookings = mutableMapOf()
            )
            is CommuteBookedEvent -> {
                val newBookings = this.bookings.toMutableMap()
                newBookings[event.bookingId.toString()] = event.seat.toString()
                this.copy(
                    bookings = newBookings
                )
            }
            is CommuteFullEvent -> this.copy(
                status = CommuteStatusEnum.FULL
            )
            is CommuteAvailableEvent -> this.copy(
                status = CommuteStatusEnum.SCHEDULED
            )
            is CommuteBookingCanceledEvent -> {
                val newBookings = this.bookings.toMutableMap()
                newBookings.remove(event.bookingId.toString())
                this.copy(
                    bookings = newBookings
                )
            }
            is CommuteExpiredEvent -> this.copy(
                status = CommuteStatusEnum.EXPIRED
            )
            // Failed events don't change the state
            is CommuteBookSeatFailedEvent,
            is CommuteExpireFailedEvent,
            is CommuteCancelBookedSeatFailedEvent -> this
            else -> this
        }
    }
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
                    commuteId = commute._id,
                    name = name,
                    departure = departure,
                    arrival = arrival,
                    seats = seats,
                )

            return commute to listOf(event)
        }
        
        fun fromEvents(events: List<CommuteEvent>): Commute? {
            if (events.isEmpty()) return null
            
            // Find the first created event
            val createdEvent = events.find { it is CommuteCreatedEvent } as? CommuteCreatedEvent
                ?: return null
                
            // Create an initial state from the created event
            var commute = Commute(
                _id = createdEvent.commuteId,
                name = createdEvent.name,
                departure = createdEvent.departure,
                arrival = createdEvent.arrival,
                seats = createdEvent.seats,
            )
            
            // Apply all events in order to reconstruct the current state
            for (event in events) {
                commute = commute.apply(event)
            }
            
            return commute
        }
    }

    fun expire(): List<CommuteEvent> {
        if (LocalDateTime.now().isBefore(this.departure.time)) {
            return listOf(CommuteExpireFailedEvent(
                commuteId = _id,
                message = "Commute $_id cannot expire before its departure time"
            ))
        }

        if (this.status == CommuteStatusEnum.EXPIRED) {
            return listOf(CommuteExpireFailedEvent(
                commuteId = _id,
                message = "Commute $_id cannot expire when not in $status status"
            ))
        }

        this.status = CommuteStatusEnum.EXPIRED

        return listOf(CommuteExpiredEvent(
            commuteId = _id,
        ))
    }

    fun bookSeat(
        bookingId: UUID,
        seat: Seat
    ): List<CommuteEvent> {
        statusCheck()
        if (this.status != CommuteStatusEnum.SCHEDULED) {
            return listOf(CommuteBookSeatFailedEvent(
                commuteId = _id,
                bookingId = bookingId,
                seat = seat,
                message = "Seat cannot be booked when Commute $_id not in SCHEDULED status, current status is $status"
            ))
        }

        if (!this.seats.contains(seat)) {
            return listOf(CommuteBookSeatFailedEvent(
                commuteId = _id,
                bookingId = bookingId,
                seat = seat,
                message = "Seat $seat not found in Commute $_id"
            ))
        }

        if (this.bookings.containsValue(seat.toString())) {
            return listOf(CommuteBookSeatFailedEvent(
                commuteId = _id,
                bookingId = bookingId,
                seat = seat,
                message = "Seat $seat already booked in Commute $_id"
            ))
        }

        this.bookings[bookingId.toString()] = seat.toString()

        return listOfNotNull(
            CommuteBookedEvent(
                commuteId = _id,
                bookingId = bookingId,
                seat = seat
            ),
            takeIf {seatsCheck()}?.let {
                CommuteFullEvent(
                    commuteId = _id,
                )
            }
        )
    }

    fun cancelBookedSeat(
        bookingId: UUID,
    ): List<CommuteEvent> {
        statusCheck()
        if (!listOf(CommuteStatusEnum.SCHEDULED, CommuteStatusEnum.FULL).contains(this.status)) {
            return listOf(CommuteCancelBookedSeatFailedEvent(
                commuteId = _id,
                bookingId = bookingId,
                message = "Cannot cancel seat booking for booking $bookingId when Commute $_id not in SCHEDULED status, current status is $status"
            ))
        }

        val seat = this.bookings.remove(bookingId.toString())
            ?: return listOf(CommuteCancelBookedSeatFailedEvent(
                commuteId = _id,
                bookingId = bookingId,
                message = "No seat assigned for booking $bookingId in Commute $_id"
            ))

        return listOfNotNull(
            CommuteBookingCanceledEvent(
                commuteId = _id,
                bookingId = bookingId,
                seat = Seat.fromString(seat)
            ),
            takeIf {seatsCheck()}?.let {
                CommuteAvailableEvent(
                    commuteId = _id
                )
            }
        )
    }

    fun compensateCancelBookedSeat(
        bookingId: UUID,
        seat: Seat
    ): List<CommuteEvent> {
        if (!this.seats.contains(seat)) {
            return listOf(CommuteBookSeatFailedEvent(
                commuteId = _id,
                bookingId = bookingId,
                seat = seat,
                message = "Seat $seat not found in Commute $_id"
            ))
        }

        if (this.bookings.containsValue(seat.toString())) {
            return listOf(CommuteBookSeatFailedEvent(
                commuteId = _id,
                bookingId = bookingId,
                seat = seat,
                message = "Seat $seat already booked in Commute $_id"
            ))
        }

        this.bookings[bookingId.toString()] = seat.toString()

        return listOfNotNull(CommuteBookedEvent(
            commuteId = _id,
            bookingId = bookingId,
            seat = seat
        ),
            takeIf {seatsCheck()}?.let {
                CommuteFullEvent(
                    commuteId = _id,
                )
            }
        )
    }

    fun compensateBookSeat(
        bookingId: UUID
    ): List<CommuteEvent> {
        val seat = this.bookings.remove(bookingId.toString())
            ?: return listOf(CommuteCancelBookedSeatFailedEvent(
                commuteId = _id,
                bookingId = bookingId,
                message = "No seat assigned for booking $bookingId in Commute $_id"
            ))

        return listOfNotNull(
            CommuteBookingCanceledEvent(
                commuteId = _id,
                bookingId = bookingId,
                seat = Seat.fromString(seat)
            ),
            takeIf {seatsCheck()}?.let {
                CommuteAvailableEvent(
                    commuteId = _id
                )
            }
        )
    }

    private fun seatsCheck() : Boolean {
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
