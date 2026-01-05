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
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferComponentId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.queryrepository.TravelOfferQueryRepository
import java.util.UUID

// TODO: can't this be in TravelOfferCommandHandler?
class TravelOfferService(
    private val travelOfferRepository: TravelOfferQueryRepository,
    private val commandBus: CommandBus,
) {
    suspend fun expireTravelOffer(id: TravelOfferComponentId, correlationId: UUID) = supervisorScope {
        travelOfferRepository.findIdsByTravelOfferComponentId(id).forEach { offerId ->
            launch {
                commandBus.dispatchAndForget(
                    ExpireTravelOfferCommand(offerId, correlationId)
                )
            }
        }
    }

    suspend fun checkTravelOfferComponentsAvailability(travelOfferId: TravelOfferId): Boolean {
        return travelOfferRepository.findStatusesOfComponents(
            travelOfferId,
        )?.let { (commuteStatus, accommodationStatus, attractionStatus) ->
            commuteStatus == CommuteStatusEnum.SCHEDULED &&
            accommodationStatus == AccommodationStatusEnum.AVAILABLE &&
            (attractionStatus == null || attractionStatus == AttractionStatusEnum.SCHEDULED)
        } ?: throw IllegalArgumentException("Invalid travel offer id: $travelOfferId")
    }

    suspend fun makeTravelOfferUnavailable(id: TravelOfferComponentId, correlationId: UUID) = supervisorScope {
        travelOfferRepository.findIdsByTravelOfferComponentId(id).forEach { offerId ->
            launch {
                commandBus.dispatchAndForget(
                    MakeTravelOfferUnavailableCommand(offerId, correlationId)
                )
            }
        }
    }

    suspend fun makeTravelOfferAvailable(id: TravelOfferComponentId, correlationId: UUID) = supervisorScope {
        travelOfferRepository.findIdsByTravelOfferComponentId(id).forEach { offerId ->
            launch {
                if (!checkTravelOfferComponentsAvailability(offerId))
                    return@launch

                commandBus.dispatchAndForget(
                    MakeTravelOfferAvailableCommand(offerId, correlationId)
                )
            }
        }
    }
}
