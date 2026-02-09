package pl.szymanski.wiktor.ta.event

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

@Serializable
sealed interface DateMetEvent : PublishableEvent

@Serializable
data class CommuteDateMetEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val commuteId: CommuteId,
) : DateMetEvent

@Serializable
data class AccommodationDateMetEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val accommodationId: AccommodationId,
) : DateMetEvent

@Serializable
data class AttractionDateMetEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val attractionId: AttractionId,
) : DateMetEvent
