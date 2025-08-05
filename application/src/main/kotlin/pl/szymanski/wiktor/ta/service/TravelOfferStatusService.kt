package pl.szymanski.wiktor.ta.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.command.MakeTravelOfferAvailableCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID
import kotlin.reflect.KClass

class TravelOfferStatusService(
    private val travelOfferRepository: TravelOfferRepository,
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
) {
    companion object {
        private val log = LoggerFactory.getLogger(TravelOfferStatusService::class.java)
    }

    suspend fun checkTravelOfferComponentsAvailability(
        travelOfferId: UUID,
    ): Boolean {
        return travelOfferRepository.findStatusesOfComponents(
            travelOfferId
        )?.let { (commuteStatus, accommodationStatus, attractionStatus) ->
            commuteStatus == CommuteStatusEnum.SCHEDULED &&
                    accommodationStatus == AccommodationStatusEnum.AVAILABLE &&
                    (attractionStatus == null || attractionStatus == AttractionStatusEnum.SCHEDULED)
        } ?: throw IllegalArgumentException("Invalid travel offer id: $travelOfferId")
    }

    suspend fun makeTravelOfferUnavailableByCommute(
        commuteId: UUID,
        correlationId: UUID,
    ) = coroutineScope {
        travelOfferRepository
            .findByCommuteId(commuteId)
            .map {
                async {
                    runCatching {
                        withRetry(3) {
                            travelOfferCommandHandler.handle(
                                MakeTravelOfferUnavailableCommand(
                                    travelOfferId = it._id,
                                    correlationId = correlationId,
                                ) as TravelOfferCommand,
                            )
                        }
                    }.exceptionOrNull()?.let { log.error("ERROR HANDLED: {}", it.message) }
                }
            }.awaitAll()
    }

    suspend fun makeTravelOfferUnavailableByAttraction(
        attractionId: UUID,
        correlationId: UUID,
    ) = coroutineScope {
        travelOfferRepository
            .findByAttractionId(attractionId)
            .map {
                async {
                    runCatching {
                        withRetry(3) {
                            travelOfferCommandHandler.handle(
                                MakeTravelOfferUnavailableCommand(
                                    travelOfferId = it._id,
                                    correlationId = correlationId,
                                ) as TravelOfferCommand,
                            )
                        }
                    }.exceptionOrNull()?.let { log.error("ERROR HANDLED: {}", it.message) }
                }
            }.awaitAll()
    }


suspend fun makeTravelOfferUnavailableByAccommodation(
        accommodationId: UUID,
        correlationId: UUID,
    ) = coroutineScope {
    travelOfferRepository
        .findByAccommodationId(accommodationId)
        .map {
            async {
                runCatching {
                    withRetry(3) {
                        travelOfferCommandHandler.handle(
                            MakeTravelOfferUnavailableCommand(
                                travelOfferId = it._id,
                                correlationId = correlationId,
                            ) as TravelOfferCommand,
                        )
                    }
                }.exceptionOrNull()?.let { log.error("ERROR HANDLED: {}", it.message) }
            }
        }.awaitAll()
    }

    suspend fun makeTravelOfferAvailableByCommute(
        commuteId: UUID,
        correlationId: UUID,
    ) = coroutineScope {
        travelOfferRepository
            .findByCommuteId(commuteId)
            .map {
                async {
                    runCatching {
                        if (!checkTravelOfferComponentsAvailability(it._id)) return@async
                        withRetry(3) {
                            travelOfferCommandHandler.handle(
                                MakeTravelOfferAvailableCommand(
                                    travelOfferId = it._id,
                                    correlationId = correlationId,
                                ) as TravelOfferCommand,
                            )
                        }
                    }.exceptionOrNull()?.let { log.error("ERROR HANDLED: {}", it.message) }
                }
            }.awaitAll()
    }

    suspend fun makeTravelOfferAvailableByAttraction(
        attractionId: UUID,
        correlationId: UUID,
    ) = coroutineScope {
        travelOfferRepository
            .findByAttractionId(attractionId)
            .map {
                async {
                    runCatching {
                        if (!checkTravelOfferComponentsAvailability(it._id)) return@async
                        withRetry(3) {
                            travelOfferCommandHandler.handle(
                                MakeTravelOfferAvailableCommand(
                                    travelOfferId = it._id,
                                    correlationId = correlationId,
                                ) as TravelOfferCommand,
                            )
                        }
                    }.exceptionOrNull()?.let { log.error("ERROR HANDLED: {}", it.message) }
                }
            }.awaitAll()
    }

    suspend fun makeTravelOfferAvailableByAccommodation(
        accommodationId: UUID,
        correlationId: UUID,
    ) = coroutineScope {
        travelOfferRepository
            .findByAccommodationId(accommodationId)
            .map {
                async {
                    runCatching {
                        if (!checkTravelOfferComponentsAvailability(it._id)) return@async
                        withRetry(3) {
                            travelOfferCommandHandler.handle(
                                MakeTravelOfferAvailableCommand(
                                    travelOfferId = it._id,
                                    correlationId = correlationId,
                                ) as TravelOfferCommand,
                            )
                        }
                    }.exceptionOrNull()?.let { log.error("ERROR HANDLED: {}", it.message) }
                }
            }.awaitAll()
    }

    suspend fun <T> withRetry(
        maxRetries: Int,
        onException: KClass<out Exception> = Exception::class,
        action: suspend () -> T
    ): T {
        var lastException: Throwable? = null
        repeat(maxRetries) {
            try {
                return action()
            } catch (e: Exception) {
                if (!onException.isInstance(e)) {
                    throw e
                }
                lastException = e
            }
        }
        log.error(lastException?.message)
        return null as T
    }
}
