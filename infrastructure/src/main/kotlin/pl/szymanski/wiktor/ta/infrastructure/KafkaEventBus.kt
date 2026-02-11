package pl.szymanski.wiktor.ta.infrastructure

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.DelayedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.DelayedEventRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties
import java.util.UUID
import kotlin.reflect.KClass

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
class KafkaEventBus(
    private val producer: KafkaProducer<String, String>,
    private val consumerFactory: (groupId: String) -> KafkaConsumer<String, String>,
    private val delayedEventRepository: DelayedEventRepository,
    private val topicResolver: (String) -> String = { type -> "events.$type" },
    private val json: Json = Json { ignoreUnknownKeys = true }
) : EventBus {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun serializePayload(event: EventEnvelope<out PublishableEvent>): String {
        val eventSerializer = (event.event::class as KClass<PublishableEvent>).serializer()
        val envelopeSerializer = EventEnvelope.serializer(eventSerializer)
        return json.encodeToString(envelopeSerializer, event as EventEnvelope<PublishableEvent>)
    }

    override suspend fun publish(event: EventEnvelope<out PublishableEvent>) = publishRaw(
        topicResolver(event.eventType),
        event.metadata.correlationId.toString(),
        serializePayload(event)
    )

    suspend fun publishRaw(topic: String, key: String, payload: String) {
        withContext(Dispatchers.IO) {
            producer.send(ProducerRecord(topic, key, payload)).get()
        }
    }

    override suspend fun publishAtGivenTime(event: EventEnvelope<out PublishableEvent>, date: LocalDateTime) {
        val topic = topicResolver(event.eventType)
        val payload = serializePayload(event)

        delayedEventRepository.save(
            DelayedEvent(
                payload = payload,
                topic = topic,
                key = event.metadata.correlationId.toString(),
                scheduledAt = date
            )
        )
    }

    override suspend fun <T : PublishableEvent> subscribe(
        eventType: KClass<T>,
        onEvent: suspend (EventEnvelope<T>) -> Unit
    ) {
        val topic = topicResolver(eventType.simpleName!!)
        val groupId = "eventbus-${eventType.simpleName}-${UUID.randomUUID()}"
        val consumer = consumerFactory(groupId)
        consumer.subscribe(listOf(topic))

        val eventSerializer = eventType.serializer()
        val envelopeSerializer = EventEnvelope.serializer(eventSerializer)

        scope.launch {
            try {
                while (isActive) {
                    val records = consumer.poll(Duration.ofMillis(500))
                    for (record in records) {
                        try {
                            val envelope = json.decodeFromString(envelopeSerializer, record.value())
                            onEvent(envelope)
                        } catch (_: Throwable) {
                            // Skip bad messages; in a real system you'd send to DLQ or log
                        }
                    }
                    consumer.commitSync()
                }
            } finally {
                runCatching { consumer.close() }
            }
        }
    }

    companion object {
        fun defaultProducer(bootstrapServers: String): KafkaProducer<String, String> {
            val props = Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.ACKS_CONFIG, "all")
                put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
            }
            return KafkaProducer(props)
        }

        fun defaultConsumerFactory(bootstrapServers: String): (String) -> KafkaConsumer<String, String> = { groupId ->
            val props = Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.toString().lowercase())
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
            }
            KafkaConsumer(props)
        }
    }
}
