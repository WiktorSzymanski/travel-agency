package pl.szymanski.wiktor.ta.presentation.controller

import com.asyncapi.kotlinasyncapi.context.service.AsyncApiExtension
import com.asyncapi.kotlinasyncapi.ktor.AsyncApiPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.query.TravelOfferQuery
import java.util.UUID
import kotlin.io.extension

// THIS SHOULD BE IN PROJECTION LAYER

fun Application.travelOfferController(
    travelOfferRepository: TravelOfferRepository,
    commuteRepository: CommuteRepository,
    accommodationRepository: AccommodationRepository,
    attractionRepository: AttractionRepository,
    travelOfferCommandHandler: TravelOfferCommandHandler,
) {
    install(AsyncApiPlugin) {
        extension =
            AsyncApiExtension.builder {
                info {
                    title("Sample API")
                    version("1.0.0")
                }
            }
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    fun extractQueryParams(queryParams: Parameters): Triple<UUID, UUID, Seat> {
        val offerId = queryParams["offerId"]?.let { UUID.fromString(it) }
        val userId = queryParams["userId"]?.let { UUID.fromString(it) }
        val seat = queryParams["seat"]?.let { Seat.fromString(it) }

        requireNotNull(offerId)
        requireNotNull(userId)
        requireNotNull(seat)

        return Triple(offerId, userId, seat)
    }

    val travelOfferQuery =
        TravelOfferQuery(
            travelOfferRepository = travelOfferRepository,
            commuteRepository = commuteRepository,
            accommodationRepository = accommodationRepository,
            attractionRepository = attractionRepository,
        )

    routing {
        get("/travelOffers") {
            val resp = travelOfferQuery.getTravelOffers()
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        get("/travelOffer/{id}") {
            val travelOfferId = call.parameters["id"]?.let { UUID.fromString(it) }

            requireNotNull(travelOfferId) {
                "Invalid travel offer Id: ${call.parameters["id"]}"
            }

            val resp = travelOfferQuery.getTravelOfferById(travelOfferId)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        get("/travelOffers/{status}") {
            val status = call.parameters["status"]?.let { TravelOfferStatusEnum.valueOf(it) }

            requireNotNull(status) {
                "Invalid travel offer status ${call.parameters["status"]}"
            }

            val resp = travelOfferQuery.getTravelOffersByStatus(status)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        post("/bookTravelOffer") {
            try {
                val (offerId, userId, seat) = extractQueryParams(call.request.queryParameters)
                travelOfferCommandHandler.handle(
                    BookTravelOfferCommand(
                        offerId,
                        UUID.randomUUID(),
                        userId,
                        seat,
                    ) as TravelOfferCommand,
                )
                call.respond(HttpStatusCode.OK, "Travel offer booked successfully")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid input parameters")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "An unexpected error occurred")
            }
        }
        post("/cancelTravelOffer") {
            try {
                val (offerId, userId, seat) = extractQueryParams(call.request.queryParameters)
                travelOfferCommandHandler.handle(
                    CancelBookTravelOfferCommand(offerId, UUID.randomUUID(), userId, seat) as TravelOfferCommand,
                )
                call.respond(HttpStatusCode.OK, "Travel offer booking cancelled successfully")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid input parameters")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "An unexpected error occurred")
            }
        }
    }

    routing {
        swaggerUI(path = "swaggerUI")
    }
}
