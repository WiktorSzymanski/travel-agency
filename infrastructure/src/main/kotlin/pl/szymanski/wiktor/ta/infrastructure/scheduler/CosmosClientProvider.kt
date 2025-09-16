package pl.szymanski.wiktor.ta.infrastructure.scheduler

import com.azure.cosmos.CosmosAsyncClient
import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.CosmosAsyncDatabase
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.CosmosItemSerializer
import com.azure.cosmos.models.CosmosContainerProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.reactor.awaitSingle
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider.PARTITION_KEY_PATH
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider.containerMono
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider.containerName
import reactor.core.publisher.Mono
import kotlin.jvm.java


class CustomItemSerializer : CosmosItemSerializer() {
    private val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule()) // java.time support
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Use ISO-8601 for LocalDate/LocalDateTime
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun <T : Any?> serialize(item: T?): Map<String?, Any?>? {
        if (item == null) return null
        val jsonString = mapper.writeValueAsString(item)
        return mapper.readValue(jsonString, Map::class.java) as Map<String?, Any?>
    }

    override fun <T : Any?> deserialize(
        jsonNodeMap: Map<String?, Any?>?,
        classType: Class<T?>?
    ): T? {
        if (jsonNodeMap == null || classType == null) return null
        val jsonString = mapper.writeValueAsString(jsonNodeMap)
        return mapper.readValue(jsonString, classType)
    }
}

object CosmosClientProjectionProvider {
    private val cosmosEndpoint = System.getenv("CosmosDBEndpoint") ?: throw kotlin.IllegalStateException("Missing CosmosDBEndpoint env var")
    private val cosmosKey = System.getenv("CosmosDBKey") ?: throw kotlin.IllegalStateException("Missing CosmosDBKey env var")
    private const val databaseName = "Projections"

    private val client: CosmosAsyncClient by lazy {
        CosmosClientBuilder()
            .endpoint(cosmosEndpoint)
            .key(cosmosKey)
            .customItemSerializer(CustomItemSerializer())
            .buildAsyncClient()
    }

    private val databaseMono: Mono<CosmosAsyncDatabase> by lazy {
        client.createDatabaseIfNotExists(databaseName)
            .map { it.properties.id }
            .map { databaseName -> client.getDatabase(databaseName) }
    }

    private val attractionContainerMono: Mono<CosmosAsyncContainer> by lazy {
        databaseMono.flatMap { database ->
            val containerProperties = CosmosContainerProperties("attractions", "/location")
            database.createContainerIfNotExists(containerProperties)
                .map { it.properties.id }
                .map { containerName -> database.getContainer(containerName) }
        }
    }

    private val commuteContainerMono: Mono<CosmosAsyncContainer> by lazy {
        databaseMono.flatMap { database ->
            val containerProperties = CosmosContainerProperties("commutes", "/arrival/location")
            database.createContainerIfNotExists(containerProperties)
                .map { it.properties.id }
                .map { containerName -> database.getContainer(containerName) }
        }
    }

    private val accommodationContainerMono: Mono<CosmosAsyncContainer> by lazy {
        databaseMono.flatMap { database ->
            val containerProperties = CosmosContainerProperties("accommodations", "/location")
            database.createContainerIfNotExists(containerProperties)
                .map { it.properties.id }
                .map { containerName -> database.getContainer(containerName) }
        }
    }

    private val travelOfferContainerMono: Mono<CosmosAsyncContainer> by lazy {
        databaseMono.flatMap { database ->
            val containerProperties = CosmosContainerProperties("travelOffer", "/id")
            database.createContainerIfNotExists(containerProperties)
                .map { it.properties.id }
                .map { containerName -> database.getContainer(containerName) }
        }
    }

    private val bookingContainerMono: Mono<CosmosAsyncContainer> by lazy {
        databaseMono.flatMap { database ->
            val containerProperties = CosmosContainerProperties("booking", "/id")
            database.createContainerIfNotExists(containerProperties)
                .map { it.properties.id }
                .map { containerName -> database.getContainer(containerName) }
        }
    }

    suspend fun getAttractionContainer(): CosmosAsyncContainer = attractionContainerMono.awaitSingle()
    suspend fun getCommuteContainer(): CosmosAsyncContainer = commuteContainerMono.awaitSingle()
    suspend fun getAccommodationContainer(): CosmosAsyncContainer = accommodationContainerMono.awaitSingle()
    suspend fun getTravelOfferContainer(): CosmosAsyncContainer = travelOfferContainerMono.awaitSingle()
    suspend fun getBookingContainer(): CosmosAsyncContainer = bookingContainerMono.awaitSingle()
}

object CosmosClientProvider {
    private val cosmosEndpoint = System.getenv("CosmosDBEndpoint") ?: throw kotlin.IllegalStateException("Missing CosmosDBEndpoint env var")
    private val cosmosKey = System.getenv("CosmosDBKey") ?: throw kotlin.IllegalStateException("Missing CosmosDBKey env var")
    private val databaseName = System.getenv("CosmosDBDatabaseName") ?: throw kotlin.IllegalStateException("Missing CosmosDBDatabaseName env var")
    private val containerName = System.getenv("CosmosDBContainerName") ?: throw kotlin.IllegalStateException("Missing CosmosDBContainerName env var")

    private const val PARTITION_KEY_PATH = "/stream"

    private val client: CosmosAsyncClient by lazy {
        CosmosClientBuilder()
            .endpoint(cosmosEndpoint)
            .key(cosmosKey)
            .customItemSerializer(CustomItemSerializer())
            .buildAsyncClient()
    }

    private val databaseMono: Mono<CosmosAsyncDatabase> by lazy {
        client.createDatabaseIfNotExists(databaseName)
            .map { it.properties.id }
            .map { databaseName -> client.getDatabase(databaseName) }
    }

    private val containerMono: Mono<CosmosAsyncContainer> by lazy {
        databaseMono.flatMap { database ->
            val containerProperties = CosmosContainerProperties(containerName, PARTITION_KEY_PATH)
            database.createContainerIfNotExists(containerProperties)
                .map { it.properties.id }
                .map { containerName -> database.getContainer(containerName) }
        }
    }

    suspend fun getContainer(): CosmosAsyncContainer = containerMono.awaitSingle()
}
