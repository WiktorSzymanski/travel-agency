package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.command.Command

fun interface CommandHandler<C : Command> {
    suspend fun handle(command: C)
}

object CommandBus {
    private val handlers = mutableMapOf<Class<out Command>, CommandHandler<*>>()

    fun <C : Command> registerHandler(commandType: Class<C>, handler: CommandHandler<C>) {
        handlers[commandType] = handler
    }

    suspend fun <C : Command> dispatch(command: C) {
        val handler = handlers[command::class.java] as? CommandHandler<C>
            ?: throw IllegalArgumentException("No handler registered for ${command::class.java}")
        handler.handle(command)
    }
}