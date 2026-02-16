package pl.szymanski.wiktor.ta.infrastructure

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.CommandHandler
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.event.DomainEvent
import kotlin.collections.get

class StandardCommandBus(
    private val bookingCommandHandler: BookingCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
) : CommandBus {
    private val handlers = mutableMapOf<Class<out Command>, CommandHandler<out Command, *>>()

    init {
        register(BookingCommand::class.java) { bookingCommandHandler.handle(it) }
        register(CommuteCommand::class.java) { commuteCommandHandler.handle(it) }
        register(AttractionCommand::class.java) { attractionCommandHandler.handle(it) }
        register(AccommodationCommand::class.java) { accommodationCommandHandler.handle(it) }
    }

    private fun <C : Command> register(commandType: Class<C>, handler: CommandHandler<C, *>) {
        handlers[commandType] = handler
    }

    override fun <C : Command, E> registerHandler(commandType: Class<C>, handler: CommandHandler<C, E>) {
        handlers[commandType] = handler
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <C : Command, E> dispatch(command: C): Triple<E, List<DomainEvent>, Metadata> {
        val handler = findHandler(command::class.java) as? CommandHandler<C, E>
            ?: throw IllegalArgumentException("No handler registered for command: ${command::class.java.name}")
        return handler.handle(command)
    }

    private fun findHandler(commandClass: Class<*>): CommandHandler<*, *>? {
        var current: Class<*>? = commandClass
        while (current != null && current != Any::class.java) {
            handlers[current]?.let { return it }
            for (iface in current.interfaces) {
                if (Command::class.java.isAssignableFrom(iface)) {
                    @Suppress("UNCHECKED_CAST")
                    handlers[iface as Class<out Command>]?.let { return it }
                }
            }
            current = current.superclass
        }
        return null
    }

    override suspend fun dispatchAndForget(command: Command) {
        dispatch<Command, Any>(command)
    }
}