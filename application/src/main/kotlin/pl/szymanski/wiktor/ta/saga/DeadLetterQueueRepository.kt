package pl.szymanski.wiktor.ta.saga

interface DeadLetterQueueRepository {
    suspend fun save(dlqEntry: DeadLetterQueueEntry)
}