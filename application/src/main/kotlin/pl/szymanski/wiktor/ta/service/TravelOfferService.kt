package pl.szymanski.wiktor.ta.service

import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferAvailableCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.queryrepository.TravelOfferQueryRepository
import java.util.UUID

class TravelOfferService(
    private val travelOfferRepository: TravelOfferQueryRepository,
    private val commandBus: CommandBus,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private suspend fun forEachOffer(
        id: UUID,
        correlationId: UUID,
        findOffers: suspend (UUID) -> List<UUID>,
        eligible: suspend (UUID) -> Boolean = { true },
        buildCommand: (UUID, UUID) -> TravelOfferCommand,
    ) = supervisorScope {
        val offerIds = findOffers(id)
        offerIds.forEach { offerId ->
            launch {
                try {
                    if (!eligible(offerId)) return@launch
                    val command = buildCommand(offerId, correlationId)
                    commandBus.dispatchAndForget(command)
                } catch (ex: Exception) {
                    log.debug(
                        "TravelOfferService batch error for offer={} corrId={}: {}",
                        offerId,
                        correlationId,
                        ex.message
                    )
                }
            }
        }
    }

    suspend fun expireTravelOfferByCommute(
        commuteId: UUID,
        correlationId: UUID,
    ) : Unit = forEachOffer(
        id = commuteId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByCommuteId,
        buildCommand = { offerId, corrId -> ExpireTravelOfferCommand(offerId, corrId) }
    )

    suspend fun expireTravelOfferByAttraction(
        attractionId: UUID,
        correlationId: UUID,
    ) : Unit = forEachOffer(
        id = attractionId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByAttractionId,
        buildCommand = { offerId, corrId -> ExpireTravelOfferCommand(offerId, corrId) }
    )

    suspend fun expireTravelOfferByAccommodation(
        accommodationId: UUID,
        correlationId: UUID,
    ) : Unit = forEachOffer(
        id = accommodationId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByAccommodationId,
        buildCommand = { offerId, corrId -> ExpireTravelOfferCommand(travelOfferId = offerId, correlationId = corrId ) }
    )

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
    ) : Unit = forEachOffer(
        id = commuteId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByCommuteId,
        buildCommand = { offerId, corrId ->
            MakeTravelOfferUnavailableCommand(
                travelOfferId = offerId,
                correlationId = corrId,
            )
        },
    )

    suspend fun makeTravelOfferUnavailableByAttraction(
        attractionId: UUID,
        correlationId: UUID,
    ) : Unit = forEachOffer(
        id = attractionId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByAttractionId,
        buildCommand = { offerId, corrId ->
            MakeTravelOfferUnavailableCommand(
                travelOfferId = offerId,
                correlationId = corrId,
            )
        },
    )

    suspend fun makeTravelOfferUnavailableByAccommodation(
        accommodationId: UUID,
        correlationId: UUID,
    ) : Unit = forEachOffer(
        id = accommodationId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByAccommodationId,
        buildCommand = { offerId, corrId ->
            MakeTravelOfferUnavailableCommand(
                travelOfferId = offerId,
                correlationId = corrId,
            )
        },
    )

    suspend fun makeTravelOfferAvailableByCommute(
        commuteId: UUID,
        correlationId: UUID,
    ) : Unit = forEachOffer(
        id = commuteId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByCommuteId,
        eligible = ::checkTravelOfferComponentsAvailability,
        buildCommand = { offerId, corrId ->
            MakeTravelOfferAvailableCommand(
                travelOfferId = offerId,
                correlationId = corrId,
            )
        },
    )

    suspend fun makeTravelOfferAvailableByAttraction(
        attractionId: UUID,
        correlationId: UUID,
    ) : Unit = forEachOffer(
        id = attractionId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByAttractionId,
        eligible = ::checkTravelOfferComponentsAvailability,
        buildCommand = { offerId, corrId ->
            MakeTravelOfferAvailableCommand(
                travelOfferId = offerId,
                correlationId = corrId,
            )
        },
    )

    suspend fun makeTravelOfferAvailableByAccommodation(
        accommodationId: UUID,
        correlationId: UUID,
    ) : Unit = forEachOffer(
        id = accommodationId,
        correlationId = correlationId,
        findOffers = travelOfferRepository::findByAccommodationId,
        eligible = ::checkTravelOfferComponentsAvailability,
        buildCommand = { offerId, corrId ->
            MakeTravelOfferAvailableCommand(
                travelOfferId = offerId,
                correlationId = corrId,
            )
        },
    )
}
