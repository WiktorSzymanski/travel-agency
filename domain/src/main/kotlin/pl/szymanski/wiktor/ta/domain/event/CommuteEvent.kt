package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

interface CommuteEvent : Event {
    val commuteId: UUID
}

data class CommuteBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
    val userId: UUID,
    val seat: Seat,
) : CommuteEvent

data class CommuteBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
    val userId: UUID,
    val seat: Seat,
) : CommuteEvent

data class CommuteExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
) : CommuteEvent