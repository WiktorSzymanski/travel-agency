package pl.szymanski.wiktor.ta.domain.event

import kotlinx.serialization.Serializable

@Serializable
sealed interface DomainEvent : PublishableEvent
