package pl.szymanski.wiktor.ta.infrastructure

import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.commands.accommodation.create.CreateAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.create.CreateAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.create.CreateCommuteCommandHandler
import pl.szymanski.wiktor.ta.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.generator.CommuteGenerator
import pl.szymanski.wiktor.ta.outbox.OutboxPort

@Component
class SchedulerService(
    private val accommodationGenerator: AccommodationGenerator,
    private val outboxPort: OutboxPort,
    private val attractionGenerator: AttractionGenerator,
    private val commuteGenerator: CommuteGenerator,
    private val createAccommodationCommandHandler: CreateAccommodationCommandHandler,
    private val createAttractionCommandHandler: CreateAttractionCommandHandler,
    private val createCommuteCommandHandler: CreateCommuteCommandHandler,
) {

    @Scheduled(fixedDelayString = "\${generator.interval-seconds:10}000")
    fun generateAccommodations() = runBlocking {
        accommodationGenerator
            .generate()
            .forEach {
                createAccommodationCommandHandler.handle(it.command)
                outboxPort.delayEvent(it.event, it.scheduleDate)
            }
    }

    @Scheduled(fixedDelayString = "\${generator.interval-seconds:10}000")
    fun generateAttractions() = runBlocking {
        attractionGenerator
            .generate()
            .forEach {
                createAttractionCommandHandler.handle(it.command)
                outboxPort.delayEvent(it.event, it.scheduleDate)
            }
    }

    @Scheduled(fixedDelayString = "\${generator.interval-seconds:10}000")
    fun generateCommutes() = runBlocking {
        commuteGenerator
            .generate()
            .forEach {
                createCommuteCommandHandler.handle(it.command)
                outboxPort.delayEvent(it.event, it.scheduleDate)
            }
    }
}

