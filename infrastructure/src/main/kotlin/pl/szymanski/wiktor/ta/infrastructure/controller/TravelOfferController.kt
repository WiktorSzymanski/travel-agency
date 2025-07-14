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
import io.ktor.server.routing.routing
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.repository.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.service.TravelOfferService

fun Application.travelOfferController() {
    install(AsyncApiPlugin) {
        extension = AsyncApiExtension.builder {
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
            }
        )
    }

    MongoDbProvider.init(property<DatabaseConfig>("database"))
    val travelOfferRepository = TravelOfferRepositoryImpl(MongoDbProvider.database)
    val travelOfferService = TravelOfferService(travelOfferRepository)
    
    val accommodationRepository = AccommodationRepositoryImpl(MongoDbProvider.database)
    val attractionRepository = AttractionRepositoryImpl(MongoDbProvider.database)
    val commuteRepository = CommuteRepositoryImpl(MongoDbProvider.database)

    routing {
        get("/travelOffers") {
            val offerDtos = travelOfferService.getTravelOffers().map {
                val accommodation = async { accommodationRepository.findById(it.accommodationId) }
                val attraction = if (it.attractionId != null) async { attractionRepository.findById(it.attractionId!!) } else null
                val commute = async { commuteRepository.findById(it.commuteId) }

                TravelOfferDto.fromDomain(it, commute.await()!!, accommodation.await()!!, attraction?.await())
            }

            call.respond(offerDtos)
        }
    }

    routing {
        swaggerUI(path = "swaggerUI")
    }
}
