package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.infrastructure.KafkaEventBus
import pl.szymanski.wiktor.ta.infrastructure.repository.DelayedEventRepository

@Configuration
class EventBusConfiguration {
    @Bean
    fun eventBus(delayedEventRepository: DelayedEventRepository): EventBus = KafkaEventBus(
        producer = KafkaEventBus.defaultProducer("localhost:9092"),
        consumerFactory = KafkaEventBus.defaultConsumerFactory("localhost:9092"),
        delayedEventRepository = delayedEventRepository
    )
}
