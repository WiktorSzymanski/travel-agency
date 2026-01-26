package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.outbox.SagaOutboxPort

class SagaService(
    private val commandBus: CommandBus,
    private val sagaRepository: SagaRepository,
    private val sagaOutboxPort: SagaOutboxPort,
    private val deadLetterQueueRepository: DeadLetterQueueRepository,
) {
    suspend fun executePendingSagas() {
        val pendingSagas =
            sagaRepository.findByStatuses(listOf(SagaStatus.NEW, SagaStatus.PROCESSING))

        pendingSagas
            .distinctBy { it.id }
            .forEach { sagaState ->
                val metadata = Metadata(sagaState.correlationId, sagaState.version.toLong())
                when (sagaState.type) {
                    SagaType.BOOKING -> BookingSaga(
                        commandBus,
                        sagaRepository,
                        sagaOutboxPort,
                        deadLetterQueueRepository,
                        sagaState,
                        metadata,
                    )
                    SagaType.CANCELLING -> CancelBookingSaga(
                        commandBus,
                        sagaRepository,
                        sagaOutboxPort,
                        deadLetterQueueRepository,
                        sagaState,
                        metadata,
                    )
                }.executeOrResume()
            }
    }
}
