package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import java.util.UUID

data class CommuteDateMetEvent(
    val eventId: UUID = UUID.randomUUID(),
    val commuteId: UUID,
    var correlationId: UUID?,
)

data class AccommodationDateMetEvent(
    val eventId: UUID = UUID.randomUUID(),
    val accommodationId: UUID,
    var correlationId: UUID?,
)

class AttractionDateMetEvent(
    val eventId: UUID = UUID.randomUUID(),
    val attractionId: UUID,
    var correlationId: UUID?,
)
