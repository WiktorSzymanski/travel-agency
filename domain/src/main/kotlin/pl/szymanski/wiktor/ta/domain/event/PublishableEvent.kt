package pl.szymanski.wiktor.ta.domain.event

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface PublishableEvent {
    val eventId: UUID
    val entityId: UUID
}
