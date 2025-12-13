package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

data class CommuteDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val commuteId: UUID,
    var correlationId: UUID?,
) : Event

data class AccommodationDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val accommodationId: UUID,
    var correlationId: UUID?,
) : Event

class AttractionDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val attractionId: UUID,
    var correlationId: UUID?,
) : Event
