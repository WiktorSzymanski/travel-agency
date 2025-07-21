package pl.szymanski.wiktor.ta.infrastructure.controller

import com.asyncapi.kotlinasyncapi.context.service.AsyncApiExtension
import com.asyncapi.kotlinasyncapi.ktor.AsyncApiPlugin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.property
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.command.travelOffer.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.travelOffer.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommand
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.event.TravelOfferEventHandler
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.repository.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.query.TravelOfferQuery
import java.util.UUID

fun Application.travelOfferController() {
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

    MongoDbProvider.init(property<DatabaseConfig>("database"))
    val travelOfferRepository = TravelOfferRepositoryImpl(MongoDbProvider.database)
    val travelOfferService = TravelOfferQuery(travelOfferRepository)

    val accommodationRepository = AccommodationRepositoryImpl(MongoDbProvider.database)
    val attractionRepository = AttractionRepositoryImpl(MongoDbProvider.database)
    val commuteRepository = CommuteRepositoryImpl(MongoDbProvider.database)

    val travelOfferCommandHandler = TravelOfferCommandHandler(travelOfferRepository)

    launch {
        TravelOfferEventHandler(
            travelOfferCommandHandler = travelOfferCommandHandler,
            attractionCommandHandler = AttractionCommandHandler(attractionRepository),
            commuteCommandHandler = CommuteCommandHandler(commuteRepository),
            accommodationCommandHandler = AccommodationCommandHandler(accommodationRepository),
        ).setup()
    }

    routing {
        get("/travelOffers") {
            val offerDtos =
                travelOfferService.getTravelOffers().map {
                    val accommodation = async { accommodationRepository.findById(it.accommodationId) }
                    val attraction = if (it.attractionId != null) async { attractionRepository.findById(it.attractionId!!) } else null
                    val commute = async { commuteRepository.findById(it.commuteId) }

                    TravelOfferDto.fromDomain(it, commute.await(), accommodation.await(), attraction?.await())
                }

            call.response.status(io.ktor.http.HttpStatusCode.OK)
            call.respond(offerDtos)
        }

        post("/bookTravelOffer") {
            val offerId = call.request.queryParameters["offerId"]?.let { UUID.fromString(it) }
            val userId = call.request.queryParameters["userId"]?.let { UUID.fromString(it) }
            val seat = call.request.queryParameters["seat"]?.let { Seat.fromString(it) }

            requireNotNull(offerId)
            requireNotNull(userId)
            requireNotNull(seat)

            travelOfferCommandHandler.handle(BookTravelOfferCommand(offerId, UUID.randomUUID(), userId, seat) as TravelOfferCommand)
        }

        post("/cancelTravelOffer") {
            val offerId = call.request.queryParameters["offerId"]?.let { UUID.fromString(it) }
            val userId = call.request.queryParameters["userId"]?.let { UUID.fromString(it) }
            val seat = call.request.queryParameters["seat"]?.let { Seat.fromString(it) }

            requireNotNull(offerId)
            requireNotNull(userId)
            requireNotNull(seat)

            travelOfferCommandHandler.handle(CancelBookTravelOfferCommand(offerId, UUID.randomUUID(), userId, seat) as TravelOfferCommand)
        }
    }

    routing {
        swaggerUI(path = "swaggerUI")
    }
}
