package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.infrastructure.KafkaEventBus

@Configuration
class EventBusConfiguration {
    // TODO: values from yaml
    @Bean
    fun eventBus(): EventBus = KafkaEventBus(
        producer = KafkaEventBus.defaultProducer("localhost:9092"),
        consumerFactory = KafkaEventBus.defaultConsumerFactory("localhost:9092"),
    )
}
