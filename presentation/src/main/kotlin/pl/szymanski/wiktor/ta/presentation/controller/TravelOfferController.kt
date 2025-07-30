package pl.szymanski.wiktor.ta.presentation.controller

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.szymanski.wiktor.ta.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.TravelOfferQueryRepository
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.query.TravelOfferQuery
import java.util.*

fun Application.travelOfferController(
    travelOfferQueryRepository: TravelOfferQueryRepository,
    accommodationQueryRepository: AccommodationQueryRepository,
    travelOfferCommandHandler: TravelOfferCommandHandler,
) {
    fun extractPaginationParams(queryParams: Parameters): Pair<Int, Int> {
        val page = queryParams["page"]?.toIntOrNull() ?: 1
        val size = queryParams["size"]?.toIntOrNull() ?: 20

        return Pair(page, size)
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
            travelOfferRepository = travelOfferQueryRepository,
            accommodationRepository = accommodationQueryRepository
        )

    routing {
        get("/travelOffers") {
            val (page, size) = extractPaginationParams(call.request.queryParameters)
            val resp = travelOfferQuery.getTravelOffers(page, size)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        get("/travelOffersUser/{userId}") {
            val (page, size) = extractPaginationParams(call.request.queryParameters)
            val userId = call.parameters["userId"]?.let { UUID.fromString(it) }

            requireNotNull(userId)
            val resp = travelOfferQuery.getTravelOfferByUserId(page, size, userId)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        get("/travelOffers/location/{location}") {
            val (page, size) = extractPaginationParams(call.request.queryParameters)
            val location = call.parameters["location"]?.let { LocationEnum.valueOf(it) }
            val status = call.request.queryParameters["status"]?.let { TravelOfferStatusEnum.valueOf(it) }
            requireNotNull(location)
            requireNotNull(status)
            val resp = travelOfferQuery.getTravelOfferByLocation(page, size, location, status)
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
            val (page, size) = extractPaginationParams(call.request.queryParameters)

            requireNotNull(status) {
                "Invalid travel offer status ${call.parameters["status"]}"
            }

            val resp = travelOfferQuery.getTravelOffersByStatus(
                status,
                page,
                size,
            )
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
                call.respond(HttpStatusCode.Accepted, "Book request accepted")
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
                call.respond(HttpStatusCode.OK, "Cancel request accepted")
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
