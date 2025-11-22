package pl.szymanski.wiktor.ta.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferAvailableCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferQueryRepository
import java.util.UUID

class TravelOfferService(
    private val travelOfferRepository: TravelOfferQueryRepository,
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
) {
    suspend fun expireTravelOfferByCommute(
        commuteId: UUID,
        correlationId: UUID,
    ) {
        travelOfferRepository
            .findByCommuteId(commuteId)
            .map {
                travelOfferCommandHandler.handle(
                    ExpireTravelOfferCommand(
                        travelOfferId = it,
                        correlationId = correlationId,
                    ) as TravelOfferCommand,
                )
            }
    }

    suspend fun expireTravelOfferByAttraction(
        attractionId: UUID,
        correlationId: UUID,
    ) {
        travelOfferRepository
            .findByAttractionId(attractionId)
            .map {
                travelOfferCommandHandler.handle(
                    ExpireTravelOfferCommand(
                        travelOfferId = it,
                        correlationId = correlationId,
                    ) as TravelOfferCommand,
                )
            }
    }

    suspend fun expireTravelOfferByAccommodation(
        accommodationId: UUID,
        correlationId: UUID,
    ) {
        travelOfferRepository
            .findByAccommodationId(accommodationId)
            .map {
                travelOfferCommandHandler.handle(
                    ExpireTravelOfferCommand(
                        travelOfferId = it,
                        correlationId = correlationId,
                    ) as TravelOfferCommand,
                )
            }
    }

    suspend fun checkTravelOfferComponentsAvailability(travelOfferId: UUID): Boolean {
        return travelOfferRepository.findStatusesOfComponents(
            travelOfferId,
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
                        travelOfferCommandHandler.handle(
                            MakeTravelOfferUnavailableCommand(
                                travelOfferId = it,
                                correlationId = correlationId,
                            ) as TravelOfferCommand,
                        )
                    }
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
                        travelOfferCommandHandler.handle(
                            MakeTravelOfferUnavailableCommand(
                                travelOfferId = it,
                                correlationId = correlationId,
                            ) as TravelOfferCommand,
                        )
                    }
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
                        travelOfferCommandHandler.handle(
                            MakeTravelOfferUnavailableCommand(
                                travelOfferId = it,
                                correlationId = correlationId,
                            ) as TravelOfferCommand,
                        )
                    }
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
                        if (!checkTravelOfferComponentsAvailability(it)) return@async
                        travelOfferCommandHandler.handle(
                            MakeTravelOfferAvailableCommand(
                                travelOfferId = it,
                                correlationId = correlationId,
                            ) as TravelOfferCommand,
                        )
                    }
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
                        if (!checkTravelOfferComponentsAvailability(it)) return@async
                        travelOfferCommandHandler.handle(
                            MakeTravelOfferAvailableCommand(
                                travelOfferId = it,
                                correlationId = correlationId,
                            ) as TravelOfferCommand,
                        )
                    }
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
                        if (!checkTravelOfferComponentsAvailability(it)) return@async
                        travelOfferCommandHandler.handle(
                            MakeTravelOfferAvailableCommand(
                                travelOfferId = it,
                                correlationId = correlationId,
                            ) as TravelOfferCommand,
                        )
                    }
                }
            }.awaitAll()
    }
}
