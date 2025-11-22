package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.Event

fun interface CommandHandler<C : Command, E> {
    suspend fun handle(command: C): Pair<E, List<Event>>
}

object CommandBus {
    private val handlers = mutableMapOf<Class<out Command>, CommandHandler<*, *>>()

    fun <C : Command, E> registerHandler(
        commandType: Class<C>,
        handler: CommandHandler<C, E>,
    ) {
        handlers[commandType] = handler
    }

    suspend fun <C : Command, E> dispatch(command: C): Pair<E, List<Event>> {
        val handler =
            handlers[command::class.java.superclass] as? CommandHandler<C, E>
                ?: throw IllegalArgumentException("No handler registered for ${command::class.java.superclass}")
        return handler.handle(command)
    }

    // TODO: shouldn't be in some CommandBus config file in infrastructure layer?
    fun setup(
        travelOfferCommandHandler: TravelOfferCommandHandler,
        bookingCommandHandler: BookingCommandHandler,
        commuteCommandHandler: CommuteCommandHandler,
        attractionCommandHandler: AttractionCommandHandler,
        accommodationCommandHandler: AccommodationCommandHandler,
    ) {
        CommandBus.registerHandler(TravelOfferCommand::class.java) {
            travelOfferCommandHandler.handle(it)
        }
        CommandBus.registerHandler(BookingCommand::class.java) {
            bookingCommandHandler.handle(it)
        }
        CommandBus.registerHandler(CommuteCommand::class.java) {
            commuteCommandHandler.handle(it)
        }
        CommandBus.registerHandler(AttractionCommand::class.java) {
            attractionCommandHandler.handle(it)
        }
        CommandBus.registerHandler(AccommodationCommand::class.java) {
            accommodationCommandHandler.handle(it)
        }
    }
}
