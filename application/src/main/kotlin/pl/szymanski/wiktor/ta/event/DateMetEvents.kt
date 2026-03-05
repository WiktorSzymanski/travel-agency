package pl.szymanski.wiktor.ta.event

import com.fasterxml.jackson.annotation.JsonIgnore
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.LocalDateTime
import java.util.UUID

sealed interface DateMetEvent : PublishableEvent {
    val date: LocalDateTime
}

data class CommuteDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val date: LocalDateTime,
    val commuteId: UUID,
) : DateMetEvent {
    @get:JsonIgnore
    override val entityId: UUID get() = commuteId
}

data class AccommodationDateMetEvent (
    override val eventId: UUID = UUID.randomUUID(),
    override val date: LocalDateTime,
    val accommodationId: UUID,
) : DateMetEvent {
    @get:JsonIgnore
    override val entityId: UUID get() = accommodationId
}

data class AttractionDateMetEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val date: LocalDateTime,
    val attractionId: UUID,
) : DateMetEvent {
    @get:JsonIgnore
    override val entityId: UUID get() = attractionId
}
