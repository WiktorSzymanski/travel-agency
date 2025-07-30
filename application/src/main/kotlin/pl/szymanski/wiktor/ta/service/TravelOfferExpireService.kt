package pl.szymanski.wiktor.ta.service

import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID

class TravelOfferExpireService(
    private val travelOfferRepository: TravelOfferRepository,
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
) {
    companion object {
        private val log = LoggerFactory.getLogger(TravelOfferExpireService::class.java)
    }

    suspend fun expireTravelOfferByCommute(
        commuteId: UUID,
        correlationId: UUID,
    ) {
        travelOfferRepository
            .findByCommuteId(commuteId)
            .map {
                try {
                    travelOfferCommandHandler.handle(
                        ExpireTravelOfferCommand(
                            travelOfferId = it._id,
                            correlationId = correlationId,
                        ) as TravelOfferCommand,
                    )
                } catch (e: IllegalArgumentException) {
                    log.error("ERROR HANDLED: {}", e.message)
                }
            }
    }

    suspend fun expireTravelOfferByAttraction(
        attractionId: UUID,
        correlationId: UUID,
    ) {
        travelOfferRepository
            .findByAttractionId(attractionId)
            .map {
                try {
                    travelOfferCommandHandler.handle(
                        ExpireTravelOfferCommand(
                            travelOfferId = it._id,
                            correlationId = correlationId,
                        ) as TravelOfferCommand,
                    )
                } catch (e: IllegalArgumentException) {
                    log.error("ERROR HANDLED: {}", e.message)
                }
            }
    }

    suspend fun expireTravelOfferByAccommodation(
        accommodationId: UUID,
        correlationId: UUID,
    ) {
        travelOfferRepository
            .findByAccommodationId(accommodationId)
            .map {
                try {
                    travelOfferCommandHandler.handle(
                        ExpireTravelOfferCommand(
                            travelOfferId = it._id,
                            correlationId = correlationId,
                        ) as TravelOfferCommand,
                    )
                } catch (e: IllegalArgumentException) {
                    log.error("ERROR HANDLED: {}", e.message)
                }
            }
    }
}
