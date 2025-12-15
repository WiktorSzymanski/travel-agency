package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

interface DateMetEvent : PublishableEvent

data class CommuteDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val commuteId: UUID,
) : DateMetEvent

data class AccommodationDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val accommodationId: UUID,
) : DateMetEvent

class AttractionDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val attractionId: UUID,
) : DateMetEvent
