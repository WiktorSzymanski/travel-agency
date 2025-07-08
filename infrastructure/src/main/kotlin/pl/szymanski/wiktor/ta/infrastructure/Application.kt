package pl.szymanski.wiktor.ta.infrastructure

import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain
        .main(args)
}

fun Application.application() {
    throw UnsupportedOperationException("Not yet implemented")
}
