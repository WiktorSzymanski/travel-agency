package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import java.util.UUID

data class CommuteDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: UUID,
    override var correlationId: UUID?
) : CommuteEvent

data class AccommodationDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: UUID,
    override var correlationId: UUID?
) : AccommodationEvent

class AttractionDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
    override var correlationId: UUID?
) : AttractionEvent
