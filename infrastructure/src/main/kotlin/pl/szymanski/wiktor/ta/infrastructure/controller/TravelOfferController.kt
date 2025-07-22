package pl.szymanski.wiktor.ta.infrastructure.controller

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
import pl.szymanski.wiktor.ta.infrastructure.repository.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.query.TravelOfferQuery
import java.util.UUID

fun Application.travelOfferController(
    travelOfferRepository: TravelOfferRepositoryImpl,
    commuteRepository: CommuteRepositoryImpl,
    accommodationRepository: AccommodationRepositoryImpl,
    attractionRepository: AttractionRepositoryImpl,
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

        post("/bookTravelOffer") {
            val (offerId, userId, seat) = extractQueryParams(call.request.queryParameters)
            try {
                travelOfferCommandHandler.handle(
                    BookTravelOfferCommand(
                        offerId,
                        UUID.randomUUID(),
                        userId,
                        seat,
                    ) as TravelOfferCommand,
                )
            } catch (e: Exception) {
                println(e)
                call.response.status(HttpStatusCode.BadRequest)
                call.respond(e.message ?: "Error while processing request")
            }
            call.response.status(HttpStatusCode.OK)
            call.respond("OK")
        }
        post("/cancelTravelOffer") {
            val (offerId, userId, seat) = extractQueryParams(call.request.queryParameters)
            try {
                travelOfferCommandHandler.handle(
                    CancelBookTravelOfferCommand(offerId, UUID.randomUUID(), userId, seat) as TravelOfferCommand,
                )
            } catch (e: Exception) {
                println(e)
                call.response.status(HttpStatusCode.BadRequest)
                call.respond(e.message ?: "Error while processing request")
            }
        }
    }

    routing {
        swaggerUI(path = "swaggerUI")
    }
}
