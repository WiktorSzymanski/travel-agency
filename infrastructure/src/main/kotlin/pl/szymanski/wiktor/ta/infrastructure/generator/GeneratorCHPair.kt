package pl.szymanski.wiktor.ta.infrastructure.generator

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import java.util.UUID

private val log = LoggerFactory.getLogger(GeneratorCHPair::class.java)

class GeneratorCHPair<T, U>(
    private val generator: Generator<T, U>,
    private val handler: Any,
) {
    suspend fun process() =
        coroutineScope {
            generator.generate().forEach { command ->
                launch {
                    runCatching {
                        when (handler) {
                            is AccommodationCommandHandler -> {
                                val event = handler.handle(command as AccommodationCommand) as AccommodationCreatedEvent
                                EventBus.publish(
                                    AccommodationDateMetEvent(
                                        event.accommodationId,
                                        UUID.randomUUID(),
                                    ),
                                    event.rent.from,
                                )
                            }
                            is AttractionCommandHandler -> {
                                val event = handler.handle(command as AttractionCommand) as AttractionCreatedEvent
                                EventBus.publish(
                                    AttractionDateMetEvent(
                                        event.attractionId,
                                        UUID.randomUUID(),
                                    ),
                                    event.date,
                                )
                            }
                            is CommuteCommandHandler -> {
                                val event = handler.handle(command as CommuteCommand) as CommuteCreatedEvent
                                EventBus.publish(
                                    CommuteDateMetEvent(
                                        event.commuteId,
                                        UUID.randomUUID(),
                                    ),
                                    event.departure.time,
                                )
                            }
                        }
                    }.onFailure { log.error("Error handling command", it) }
                }
            }
        }
}
