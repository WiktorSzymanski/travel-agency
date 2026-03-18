package pl.szymanski.wiktor.ta.infrastructure.config

import tools.jackson.databind.json.JsonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@Configuration
@EnableKafka
class KafkaConfiguration {

    @Bean
    fun consumerFactory(objectMapper: JsonMapper): ConsumerFactory<String, Any> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ConsumerConfig.GROUP_ID_CONFIG to "travel-agency",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JacksonJsonDeserializer::class.java,
            JacksonJsonDeserializer.TRUSTED_PACKAGES to "*",
            JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS to "true",
        )
        val deserializer = JacksonJsonDeserializer<Any>(Any::class.java, objectMapper)
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), deserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        kafkaTemplate: KafkaTemplate<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.setConsumerFactory(consumerFactory)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        factory.containerProperties.isAsyncAcks = false

        val backOff = ExponentialBackOffWithMaxRetries(5).apply {
            initialInterval = 500L
            multiplier = 2.0
            maxInterval = 10_000L
        }

        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        factory.setCommonErrorHandler(DefaultErrorHandler(recoverer, backOff))
        return factory
    }

    @Bean
    fun producerFactory(objectMapper: JsonMapper): ProducerFactory<String, Any> {
        val props = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonJsonSerializer::class.java,
        )
        val serializer = JacksonJsonSerializer<Any>(objectMapper)
        return DefaultKafkaProducerFactory(props, StringSerializer(), serializer)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>) =
        KafkaTemplate(producerFactory)
}
