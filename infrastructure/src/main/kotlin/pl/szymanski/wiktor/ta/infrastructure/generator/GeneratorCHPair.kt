package pl.szymanski.wiktor.ta.infrastructure.generator

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommand
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommand
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.commute.CommuteCommand
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler

class GeneratorCHPair<T, U>(
    private val generator: Generator<T, U>,
    private val commandHandler: Any,
) where U : Any {
    suspend fun process() =
        coroutineScope {
            generator.generate().forEach { command ->
                launch {
                    when (commandHandler) {
                        is AccommodationCommandHandler -> {
                            commandHandler.handle(command as AccommodationCommand)
                        }
                        is AttractionCommandHandler -> {
                            commandHandler.handle(command as AttractionCommand)
                        }
                        is CommuteCommandHandler -> {
                            commandHandler.handle(command as CommuteCommand)
                        }
                    }
                }
            }
        }
}

