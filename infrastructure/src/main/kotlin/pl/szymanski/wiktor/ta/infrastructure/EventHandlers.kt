package pl.szymanski.wiktor.ta.infrastructure

import io.ktor.server.application.Application
import kotlinx.coroutines.runBlocking

fun Application.eventHandlers(
    dateMetEventHandler: DateMetEventHandler
) {
    // Initialize subscriptions at startup
    runBlocking {
        dateMetEventHandler.subscribe()
    }
}
