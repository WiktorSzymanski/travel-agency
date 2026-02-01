package pl.szymanski.wiktor.ta.dlq

interface DeadLetterQueueRepository {
    suspend fun save(dlqEntry: DeadLetterQueueEntry)
}
