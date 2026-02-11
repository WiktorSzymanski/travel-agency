package pl.szymanski.wiktor.ta.infrastructure.config

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.infrastructure.KafkaEventBus
import pl.szymanski.wiktor.ta.infrastructure.repository.DelayedEventRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDelayedEventRepository

fun provideDelayedEventRepository(databaseProvider: DatabaseProvider): DelayedEventRepository =
    MongoDelayedEventRepository(databaseProvider)

fun provideEventBus(delayedEventRepository: DelayedEventRepository): EventBus = KafkaEventBus(
    producer = KafkaEventBus.defaultProducer("localhost:9092"),
    consumerFactory = KafkaEventBus.defaultConsumerFactory("localhost:9092"),
    delayedEventRepository = delayedEventRepository
)
