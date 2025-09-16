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
    val lastRevision: Int = -1,
    val lastEtag : String? = null
) {
    fun apply(event: CommuteEvent, revision: Int, etag: String): Commute {
        return when (event) {
            is CommuteCreatedEvent -> this.copy(
                id = event.commuteId,
                name = event.name,
                departure = event.departure,
                arrival = event.arrival,
                seats = event.seats,
                status = CommuteStatusEnum.SCHEDULED,
                bookings = mutableMapOf(),
                lastRevision = revision,
                lastEtag = etag
            )
            is CommuteBookedEvent -> {
                val newBookings = this.bookings.toMutableMap()
                newBookings[event.bookingId.toString()] = event.seat.toString()
                this.copy(
                    bookings = newBookings,
                    lastRevision = revision,
                    lastEtag = etag
                )
            }
            is CommuteFullEvent -> this.copy(
                status = CommuteStatusEnum.FULL,
                lastRevision = revision,
                lastEtag = etag
            )
            is CommuteAvailableEvent -> this.copy(
                status = CommuteStatusEnum.SCHEDULED,
                lastRevision = revision,
                lastEtag = etag
            )
            is CommuteBookingCanceledEvent -> {
                val newBookings = this.bookings.toMutableMap()
                newBookings.remove(event.bookingId.toString())
                this.copy(
                    bookings = newBookings,
                    lastRevision = revision,
                    lastEtag = etag
                )
            }
            is CommuteExpiredEvent -> this.copy(
                status = CommuteStatusEnum.EXPIRED,
                lastRevision = revision,
                lastEtag = etag
            )
            else -> this.copy(lastRevision = revision,
                lastEtag = etag)
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
                    commuteId = commute.id,
                    name = name,
                    departure = departure,
                    arrival = arrival,
                    seats = seats,
                )

            return commute to listOf(event)
        }
        
        fun fromEvents(events: List<Triple<CommuteEvent, Int, String>>): Commute? {
            if (events.isEmpty()) return null

            val (createdEvent, _) = events.first()

            require(createdEvent is CommuteCreatedEvent) { "First event must be CommuteCreatedEvent" }

            var commute = Commute(
                id = createdEvent.commuteId,
                name = createdEvent.name,
                departure = createdEvent.departure,
                arrival = createdEvent.arrival,
                seats = createdEvent.seats,
            )

            for ((event, revision, etag) in events) {
                commute = commute.apply(event, revision, etag)
            }
            
            return commute
        }
    }

    fun expire(): List<CommuteEvent> {
        require(!LocalDateTime.now().isBefore(this.departure.time)) {
            "Commute $id cannot expire before its departure time"
        }

        require(this.status != CommuteStatusEnum.EXPIRED) {
            "Commute $id cannot expire when not in $status status"
        }

        this.status = CommuteStatusEnum.EXPIRED

        return listOf(CommuteExpiredEvent(
            commuteId = id,
        ))
    }

    fun bookSeat(
        bookingId: UUID,
        seat: Seat? = null,
    ): List<CommuteEvent> {
        statusCheck()
        require(this.status == CommuteStatusEnum.SCHEDULED) {
            "Seat cannot be booked when Commute $id not in SCHEDULED status, current status is $status"
        }

        val seatToBook = when (seat) {
            null -> {
                val availableSeats = this.seats.filter { !this.bookings.containsValue(it.toString()) }
                require(availableSeats.isNotEmpty()) {
                    "No available seats in Commute $id"
                }
                availableSeats[0]
            }
            is Seat -> {
                require(this.seats.contains(seat)) {
                    "Seat $seat not found in Commute $id"
                }

                require(!this.bookings.containsValue(seat.toString())) {
                    "Seat $seat already booked in Commute $id"
                }

                seat
            }
        }

        this.bookings[bookingId.toString()] = seatToBook.toString()

        return listOfNotNull(
            CommuteBookedEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = seatToBook
            ),
            takeIf {seatsCheck()}?.let {
                CommuteFullEvent(
                    commuteId = id,
                )
            }
        )
    }

    fun cancelBookedSeat(
        bookingId: UUID,
    ): List<CommuteEvent> {
        statusCheck()
        require(listOf(CommuteStatusEnum.SCHEDULED, CommuteStatusEnum.FULL).contains(this.status)) {
            "Cannot cancel seat booking for booking $bookingId when Commute $id not in SCHEDULED status, current status is $status"
        }

        val seat = this.bookings.remove(bookingId.toString())
        require( seat != null) {
            "No seat assigned for booking $bookingId in Commute $id"
        }

        return listOfNotNull(
            CommuteBookingCanceledEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = Seat.fromString(seat)
            ),
            takeIf {seatsCheck()}?.let {
                CommuteAvailableEvent(
                    commuteId = id
                )
            }
        )
    }

    fun compensateCancelBookedSeat(
        bookingId: UUID,
        seat: Seat
    ): List<CommuteEvent> {
        require(this.seats.contains(seat)) {
            "Seat $seat not found in Commute $id"
        }

        require(!this.bookings.containsValue(seat.toString())) {
            "Seat $seat already booked in Commute $id"
        }

        this.bookings[bookingId.toString()] = seat.toString()

        return listOfNotNull(CommuteBookedEvent(
            commuteId = id,
            bookingId = bookingId,
            seat = seat
        ),
            takeIf {seatsCheck()}?.let {
                CommuteFullEvent(
                    commuteId = id,
                )
            }
        )
    }

    fun compensateBookSeat(
        bookingId: UUID
    ): List<CommuteEvent> {
        val seat = this.bookings.remove(bookingId.toString())
        require( seat != null) {
            "No seat assigned for booking $bookingId in Commute $id"
        }

        return listOfNotNull(
            CommuteBookingCanceledEvent(
                commuteId = id,
                bookingId = bookingId,
                seat = Seat.fromString(seat)
            ),
            takeIf {seatsCheck()}?.let {
                CommuteAvailableEvent(
                    commuteId = id
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