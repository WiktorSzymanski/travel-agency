package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateAttractionCommand
import pl.szymanski.wiktor.ta.command.CompensateCommuteCommand
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.event.DomainEvent
import java.util.UUID
import kotlin.collections.get

class DummyCommandBus : CommandBus {
    private val handlers = mutableMapOf<Class<out Command>, CommandHandler<*, *>>()

    override fun <C : Command, E> registerHandler(
        commandType: Class<C>,
        handler: CommandHandler<C, E>,
    ) {
        handlers[commandType] = handler
    }

    override suspend fun <C : Command, E> dispatch(command: C): Triple<E, List<DomainEvent>, Metadata> {
        val handler =
            handlers[command::class.java.superclass] as? CommandHandler<C, E>
                ?: throw IllegalArgumentException("No handler registered for ${command::class.java.superclass}")
        return handler.handle(command)
    }

    override suspend fun dispatchAndForget(command: Command) {
        val handler =
            handlers[command::class.java.superclass] as? CommandHandler<Command, Any>
                ?: throw IllegalArgumentException("No handler registered for ${command::class.java.superclass}")
        handler.handle(command)
    }

    constructor()

    constructor(
        bookingCommandHandler: BookingCommandHandler,
        commuteCommandHandler: CommuteCommandHandler,
        attractionCommandHandler: AttractionCommandHandler,
        accommodationCommandHandler: AccommodationCommandHandler,
    ) {
        this.registerHandler(BookingCommand::class.java) { command ->
            bookingCommandHandler.handle(command)
        }
        this.registerHandler(CommuteCommand::class.java) {  command ->
            commuteCommandHandler.handle(command)
        }
        this.registerHandler(AttractionCommand::class.java) {  command ->
            attractionCommandHandler.handle(command)
        }
        this.registerHandler(AccommodationCommand::class.java) {  command ->
            accommodationCommandHandler.handle(command)
        }


        this.registerHandler(CompensateCommuteCommand::class.java) {  command ->
            commuteCommandHandler.handle(command)
        }
        this.registerHandler(CompensateAttractionCommand::class.java) {  command ->
            attractionCommandHandler.handle(command)
        }
        this.registerHandler(CompensateAccommodationCommand::class.java) {  command ->
            accommodationCommandHandler.handle(command)
        }
    }
}