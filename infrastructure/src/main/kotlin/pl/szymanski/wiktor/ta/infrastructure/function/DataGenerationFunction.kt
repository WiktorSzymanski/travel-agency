package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.OutputBinding
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.CosmosDBTrigger
import com.microsoft.azure.functions.annotation.EventGridOutput
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.TimerTrigger
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.infrastructure.config.DataGenerationSchedulerConfig
import pl.szymanski.wiktor.ta.infrastructure.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.AccommodationTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.AttractionTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.CommuteGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.CommuteTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.GeneratorCHPair
import pl.szymanski.wiktor.ta.infrastructure.projection.AccommodationProjectionService
import pl.szymanski.wiktor.ta.infrastructure.projection.AttractionProjectionService
import pl.szymanski.wiktor.ta.infrastructure.projection.CommuteProjectionService
import pl.szymanski.wiktor.ta.infrastructure.projection.TravelOfferProjectionService
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.command.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.EventRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.AccommodationQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.AttractionQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.CommuteQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import pl.szymanski.wiktor.ta.offerMaker.OfferMaker
import pl.szymanski.wiktor.ta.query.TravelOfferQuery
import java.util.Optional

@FunctionName("MakeOffers")
fun makeOffers(
    @TimerTrigger(name = "timerInfo", schedule = "0 */1 * * * *")
    timerInfo: String,
    context: ExecutionContext
) = runBlocking {
    val accommodationRepository = AccommodationQueryRepositoryImpl()
    val attractionRepository = AttractionQueryRepositoryImpl()
    val commuteRepository = CommuteQueryRepositoryImpl()
    val travelOfferCommandHandler = TravelOfferCommandHandler(TravelOfferRepositoryImpl())

    val offerMaker =
        OfferMaker(
            accommodationRepository,
            attractionRepository,
            commuteRepository,
            travelOfferCommandHandler,
        )

    offerMaker.makeOffers()
}

@FunctionName("GenerateData")
fun generateData(
    @TimerTrigger(name = "timerInfo", schedule = "0 */1 * * * *")
    timerInfo: String,
    context: ExecutionContext
) = runBlocking {
    val container = CosmosClientProvider.getContainer()
    EventBus.init(EventRepositoryImpl(container))
    context.logger.info("HELLO THERE")

    val config = DataGenerationSchedulerConfig(
        intervalSeconds = 1.0,
        inAdvanceSeconds = 300,
        creationWindowSeconds = 10,
        accommodations = listOf(
            AccommodationTemplate(
                "Hotel",
                "LONDON"
            )
        ),
        attractions = listOf(
            AttractionTemplate(
                "Big Ben tour",
                "LONDON",
                3
            )
        ),
        commutes = listOf(
            CommuteTemplate(
                "Poznan to London",
                "POZNAN",
                "LONDON",
                listOf(
                    Seat("A", "1"),
                    Seat("A", "2")
                )
            )
        ),
    )
    val accommodationCommandHandler = AccommodationCommandHandler(
        AccommodationRepositoryImpl()
    )
    val attractionCommandHandler = AttractionCommandHandler(
        AttractionRepositoryImpl()
    )
    val commuteCommandHandler = CommuteCommandHandler(
        CommuteRepositoryImpl()
    )

    val generators =
        listOf(
            GeneratorCHPair(
                CommuteGenerator(
                    config.inAdvanceSeconds,
                    config.creationWindowSeconds,
                    config.commutes,
                ),
                commuteCommandHandler,
            ),
            GeneratorCHPair(
                AccommodationGenerator(
                    config.inAdvanceSeconds,
                    config.creationWindowSeconds,
                    config.accommodations,
                ),
                accommodationCommandHandler,
            ),
            GeneratorCHPair(
                AttractionGenerator(
                    config.inAdvanceSeconds,
                    config.creationWindowSeconds,
                    config.attractions,
                ),
                attractionCommandHandler,
            ),
        )

    generators.forEach { it.process() }
}

val travelOfferQueryRepository = TravelOfferQueryRepositoryImpl()
val accommodationQueryRepository = AccommodationQueryRepositoryImpl()

val travelOfferQuery = TravelOfferQuery(
    travelOfferQueryRepository,
    accommodationQueryRepository
)

val accommodationProjectionService = AccommodationProjectionService(accommodationQueryRepository)
val commuteProjectionService = CommuteProjectionService(CommuteQueryRepositoryImpl())
val attractionProjectionService = AttractionProjectionService(AttractionQueryRepositoryImpl())
val travelOfferProjectionService = TravelOfferProjectionService(travelOfferQueryRepository)

val commuteCommandHandler = CommuteCommandHandler(CommuteRepositoryImpl())
val accommodationCommandHandler = AccommodationCommandHandler(AccommodationRepositoryImpl())
val attractionCommandHandler = AttractionCommandHandler(AttractionRepositoryImpl())
val travelOfferCommandHandler = TravelOfferCommandHandler(TravelOfferRepositoryImpl())

@FunctionName("PublishEventsToGrid")
fun publishEventsToGrid(
    @CosmosDBTrigger(
        name = "projectionsUpdate",
        databaseName = "%CosmosDbDatabaseName%",
        containerName = "%CosmosDBContainerName%",
        connection = "CosmosConnectionString",
        leaseContainerName = "leases",
        createLeaseContainerIfNotExists = true
    )
    input: List<PersistedEvent>,
    @EventGridOutput(
        name = "outputEvent",
        topicEndpointUri = "EventGridTopicUri",
        topicKeySetting = "EventGridTopicKey"
    )
    outputEvent: OutputBinding<List<PersistedEvent>>,
    context: ExecutionContext
) {
    context.logger.info("Got ${input.size} events to publish to EventGrid")
    outputEvent.value = input
}

@FunctionName("ProjectionUpdateFunction")
fun projectionUpdateFunction(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
        val clazz = Class.forName(event.type)
        when {
             AccommodationEvent::class.java.isAssignableFrom(clazz) -> runBlocking {
                context.logger.info("In AccommodationEvent block!!! with ${event.type}")
                 accommodationProjectionService.updateProjection(
                    EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as AccommodationEvent,
                     event.revision.toInt()
                )
            }
            CommuteEvent::class.java.isAssignableFrom(clazz) -> runBlocking {
                context.logger.info { "In CommuteEvent block!!! with ${event.type}" }
                commuteProjectionService.updateProjection(
                    EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as CommuteEvent,
                    event.revision.toInt(),
                    context
                )
            }
            AttractionEvent::class.java.isAssignableFrom(clazz) -> runBlocking {
                context.logger.info { "In AttractionEvent block!!! with ${event.type}" }
                attractionProjectionService.updateProjection(
                    EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as AttractionEvent,
                    event.revision.toInt()
                )
            }
            TravelOfferEvent::class.java.isAssignableFrom(clazz) -> runBlocking {
                context.logger.info { "In TravelOffer block!!! with ${event.type}" }
                travelOfferProjectionService.updateProjection(
                    EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as TravelOfferEvent,
                    event.revision.toInt()
                )
            }
            else -> context.logger.info { "In else block!!! with ${event.type}" }
        }
}

@FunctionName("GetTravelOffersByStatus")
fun getTravelOffersByStatus(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "travelOffers/{status}"
    )
    request: HttpRequestMessage<Optional<String>>,
    @BindingName("status") statusParam: String,
    context: ExecutionContext
): HttpResponseMessage {

    val logger = context.logger
    logger.info("Received request for travel offers with status: $statusParam")

    val status = try {
        TravelOfferStatusEnum.valueOf(statusParam)
    } catch (ex: IllegalArgumentException) {
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("Invalid status parameter: $statusParam")
            .build()
    }

    val queryParams = request.queryParameters
    val page = queryParams["page"]?.toIntOrNull() ?: 1
    val size = queryParams["size"]?.toIntOrNull() ?: 10

    val response = runBlocking {
        travelOfferQuery.getTravelOffersByStatus(status, page, size)
    }

    return request.createResponseBuilder(HttpStatus.OK)
        .header("Content-Type", "application/json")
        .body(response)
        .build()
}