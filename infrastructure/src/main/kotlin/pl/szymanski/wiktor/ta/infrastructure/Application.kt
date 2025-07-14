package pl.szymanski.wiktor.ta.infrastructure

import io.ktor.server.application.Application
import pl.szymanski.wiktor.ta.infrastructure.controller.travelOfferController

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain
        .main(args)
}

fun Application.application(): Unit {
    travelOfferController()
}
