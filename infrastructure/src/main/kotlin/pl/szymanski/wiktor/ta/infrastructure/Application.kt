package pl.szymanski.wiktor.ta.infrastructure

import io.ktor.server.application.Application
import io.ktor.server.cio.EngineMain
import pl.szymanski.wiktor.ta.infrastructure.controller.travelOfferController

fun main(args: Array<String>) {
    EngineMain
        .main(args)
}

fun Application.application() {
    travelOfferController()
}
