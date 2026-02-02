package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

interface DateMetEvent : PublishableEvent

data class CommuteDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val commuteId: CommuteId,
) : DateMetEvent

data class AccommodationDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val accommodationId: AccommodationId,
) : DateMetEvent

data class AttractionDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val attractionId: AttractionId,
) : DateMetEvent
