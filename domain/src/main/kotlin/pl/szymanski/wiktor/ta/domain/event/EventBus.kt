package pl.szymanski.wiktor.ta.domain.event

interface EventBus {
    suspend fun publish(event: DomainEvent)
}