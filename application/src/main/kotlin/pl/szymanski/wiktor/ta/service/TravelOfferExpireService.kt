package pl.szymanski.wiktor.ta.service

import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID

class TravelOfferExpireService(
    private val travelOfferRepository: TravelOfferRepository,
    private val travelOfferCommandHandler: TravelOfferCommandHandler
) {
    suspend fun expireTravelOfferByCommute(commuteId: UUID, correlationId: UUID) {
        travelOfferRepository
            .findByCommuteId(commuteId)
            .map {
                try {
                    travelOfferCommandHandler.handle(
                        ExpireTravelOfferCommand(
                            travelOfferId = it._id,
                            correlationId = correlationId,
                        ) as TravelOfferCommand
                    )
                } catch (e: IllegalArgumentException) {
                    println("ERROR HANDLED: $e")
                }
            }
    }

    suspend fun expireTravelOfferByAttraction(attractionId: UUID, correlationId: UUID) {
        travelOfferRepository
            .findByAttractionId(attractionId)
            .map { try {
                travelOfferCommandHandler.handle(
                    ExpireTravelOfferCommand(
                        travelOfferId = it._id,
                        correlationId = correlationId,
                    ) as TravelOfferCommand
                ) } catch (e: IllegalArgumentException) {
                    println("ERROR HANDLED: $e")
                }
            }
    }

    suspend fun expireTravelOfferByAccommodation(accommodationId: UUID, correlationId: UUID) {
        travelOfferRepository
            .findByAccommodationId(accommodationId)
            .map {
                try {
                    travelOfferCommandHandler.handle(
                        ExpireTravelOfferCommand(
                            travelOfferId = it._id,
                            correlationId = correlationId,
                        ) as TravelOfferCommand
                    )
                } catch (e: IllegalArgumentException) {
                    println("ERROR HANDLED: $e")
                }
            }
    }
}