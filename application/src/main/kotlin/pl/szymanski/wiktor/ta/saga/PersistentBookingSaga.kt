package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.delay
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookCommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateCommuteCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import java.util.UUID
import kotlin.reflect.KClass

class PersistentBookingSaga(
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val stateRepo: SagaRepository,
    private val sagaState: SagaState,
    private val metadata: Metadata
) {
    private val accommodationCommand: AccommodationCommand =
        BookAccommodationCommand(
            sagaState.travelOffer.accommodationId,
            metadata.correlationId,
            sagaState.bookingId,
        )

    private fun getCompensateAccommodationCommand(eventId: UUID): CompensateAccommodationCommand {
        return CompensateBookAccommodationCommand(
            accommodationId = sagaState.travelOffer.accommodationId,
            correlationId = metadata.correlationId,
            eventId = eventId,
            bookingId = sagaState.bookingId,
        )
    }

    private var commuteCommand: CommuteCommand =
        BookCommuteCommand(
            sagaState.travelOffer.commuteId,
            metadata.correlationId,
            sagaState.bookingId,
            sagaState.seat,
        )

    private fun getCompensateCommuteCommand(eventId: UUID): CompensateCommuteCommand {
        return CompensateBookCommuteCommand(
            commuteId = sagaState.travelOffer.commuteId,
            correlationId = metadata.correlationId,
            eventId = eventId,
            bookingId = sagaState.bookingId,
        )
    }

    private val attractionCommand: AttractionCommand? =
        when (val attractionId = sagaState.travelOffer.attractionId) {
            is AttractionId.Present -> BookAttractionCommand(
                attractionId,
                metadata.correlationId,
                sagaState.bookingId,
            )
            is AttractionId.Empty -> null
        }

    private val maxRetries = 5

    suspend fun executeOrResume() {
        when (sagaState.status) {
            SagaStatus.COMPLETED -> return
            SagaStatus.FAILED -> return
            SagaStatus.COMPENSATING -> compensateAndFail(sagaState.message!!)
            SagaStatus.PROCESSING -> resume()
            SagaStatus.NEW -> startSaga()
        }
    }

    suspend fun resume() {
        when (sagaState.step) {
            SagaStep.PENDING_COMMUTE -> handleCommuteStep()
            SagaStep.PENDING_ACCOMMODATION -> handleAccommodationStep()
            SagaStep.PENDING_ATTRACTION -> handleAttractionStep()
            SagaStep.COMPENSATING_ACCOMMODATION -> compensateAccommodation()
            SagaStep.COMPENSATING_COMMUTE -> compensateCommute()
            SagaStep.IDLE -> return
        }
    }

    private suspend fun startSaga() {
        stateRepo.save(sagaState)
        // TODO: they both should be persisted at the same time
        eventBus.publish(EventEnvelope(BookingSagaStartedEvent(
            bookingId = sagaState.bookingId
        ), metadata))
        sagaState.status = SagaStatus.PROCESSING
        sagaState.step = SagaStep.PENDING_COMMUTE
        persistState()
        handleCommuteStep()
    }

    private suspend fun handleCommuteStep() {
        val step = runCatching {
            sagaStep(
                sagaState = sagaState,
                init = { },
                loop = {
                    val events = commandBus.dispatch<CommuteCommand, Commute>(commuteCommand).second
                    sagaState.sagaContext.commuteEventId = events.first().eventId
                    sagaState.step = SagaStep.PENDING_ACCOMMODATION
                    sagaState.retryCount = 0
                    persistState()
                },
            )
        }

        if (step.isFailure)
            return compensateAndFail(step.exceptionOrNull()!!.message!!)

        handleAccommodationStep()
    }

    private suspend fun handleAccommodationStep() {
        val step = runCatching {
            sagaStep(
                sagaState = sagaState,
                init = { },
                loop = {
                    val events = commandBus.dispatch<AccommodationCommand, Accommodation>(accommodationCommand).second
                    sagaState.sagaContext.accommodationEventId = events.first().eventId
                    sagaState.step = SagaStep.PENDING_ATTRACTION
                    sagaState.retryCount = 0
                    persistState()
                }
            )
        }

        if (step.isFailure)
            return compensateAndFail(step.exceptionOrNull()!!.message!!)

        handleAttractionStep()
    }

    private suspend fun handleAttractionStep() {
        attractionCommand?.let {
            val step = runCatching {
                sagaStep(
                    sagaState = sagaState,
                    init = { },
                    loop = {
                        commandBus.dispatch<AttractionCommand, Attraction>(it)
                    }
                )
            }

            if (step.isFailure)
                return compensateAndFail(step.exceptionOrNull()!!.message!!)
        }

        completeSaga()
    }

    private suspend fun completeSaga() {
        sagaState.status = SagaStatus.COMPLETED
        persistState()
        eventBus.publish(EventEnvelope(BookingSagaCompletedEvent(
            bookingId = sagaState.bookingId,
            seat = sagaState.seat),
            metadata)
        )
    }

    private suspend fun compensateAndFail(message: String) {
        eventBus.publish(EventEnvelope(BookingSagaFailedEvent(
            bookingId = sagaState.bookingId,
            message = message), metadata))
        sagaState.message = message
        persistState()
        compensateAccommodation()
        compensateCommute()
        sagaState.status = SagaStatus.FAILED
        sagaState.step = SagaStep.IDLE
        persistState()
    }

    private suspend fun compensateCommute() {
        sagaState.sagaContext.commuteEventId?.let {
            val step = runCatching {
                sagaStep(
                    sagaState = sagaState,
                    init = {
                        sagaState.step = SagaStep.COMPENSATING_COMMUTE
                        persistState()
                    },
                    loop = {
                        commandBus.dispatch<CommuteCommand, Commute>(getCompensateCommuteCommand(it))
                    },
                )
            }

            if (step.isFailure)
                return // ADD RECORD TO UNPROCESSABLE SAGAS
        }
    }

    private suspend fun compensateAccommodation() {
        sagaState.sagaContext.accommodationEventId?.let {
            val step = runCatching {
                sagaStep(
                    sagaState = sagaState,
                    init = {
                        sagaState.step = SagaStep.COMPENSATING_ACCOMMODATION
                        persistState()
                    },
                    loop = {
                        commandBus.dispatch<AccommodationCommand, Accommodation>(getCompensateAccommodationCommand(it))
                    }
                )
            }

            if (step.isFailure)
                return // ADD RECORD TO UNPROCESSABLE SAGAS
        }
    }

    private suspend fun persistState() {
        this.sagaState.incrementVersion()
        stateRepo.save(this.sagaState)
    }

    private suspend fun sagaStep(
        sagaState: SagaState,
        init: suspend () -> Unit,
        loop: suspend () -> Unit,
        exception: KClass<out Exception> = ConcurrentModificationException::class,
        delayManager: DelayManager = DelayManager()
    ): Boolean {
        var lastException: Throwable? = null

        init()

        repeat(maxRetries - sagaState.retryCount) { attemptNo ->
            val result = runCatching { loop() }
            if (result.isSuccess) return true

            lastException = result.exceptionOrNull()
            if (!exception.isInstance(lastException)) throw lastException!!

            if (attemptNo < maxRetries - sagaState.retryCount - 1)
                delay(delayManager.getCurrentDelay())
        }

        throw lastException!!
    }
}
