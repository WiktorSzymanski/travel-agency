package pl.szymanski.wiktor.ta.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnore
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.LocalDateTime
import java.util.UUID


sealed interface AggregateId2 {
    val value: UUID
}

sealed interface TravelOfferComponentId2: AggregateId2

@JvmInline
value class CommuteId2(override val value: UUID) : TravelOfferComponentId2 {
    companion object {
        fun generate(): CommuteId = CommuteId(UUID.randomUUID())
        fun from(value: UUID): CommuteId = CommuteId(value)
    }
}

sealed interface DateMetEvent2 : PublishableEvent {
    val date: LocalDateTime
}

sealed interface DomainEvent2 : PublishableEvent


data class CommuteDateMetEvent2 (
    override val eventId: UUID = UUID.randomUUID(),
    override val date: LocalDateTime,
    val commuteId: CommuteId,
) : DateMetEvent2 {
    @get:JsonIgnore
    override val entityId: UUID get() = commuteId.value
}

data class AttractionDateMetEvent2 (
    override val eventId: UUID = UUID.randomUUID(),
    override val date: LocalDateTime,
    val attractionId: UUID,
) : DateMetEvent2 {
    @get:JsonIgnore
    override val entityId: UUID get() = attractionId

//    fun attractionIdDomain(): AttractionId = AttractionId.Present(attractionId)
}

data class AccommodationDateMetEvent2 (
    override val eventId: UUID = UUID.randomUUID(),
    override val date: LocalDateTime,
    val accommodationId: AccommodationId,
) : DateMetEvent2 {
    @get:JsonIgnore
    override val entityId: UUID get() = accommodationId.value
}

data class EventEnvelope2<T: PublishableEvent> (
    val eventType: String,
    val event: T,
    val metadata: Metadata,
) {
    constructor(event: T, metadata: Metadata) : this(
        event::class.simpleName!!,
        event,
        metadata
    )
}
