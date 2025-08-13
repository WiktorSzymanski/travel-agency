package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository

/**
 * Event handler that subscribes to all entity events and saves them using the appropriate repository.
 * This replaces direct calls to repository update and save methods in command handlers.
 */
class EntityEventHandler(
    private val accommodationRepository: AccommodationRepository,
    private val attractionRepository: AttractionRepository,
    private val commuteRepository: CommuteRepository,
    private val travelOfferRepository: TravelOfferRepository
) {
    companion object {
        private val log = LoggerFactory.getLogger(EntityEventHandler::class.java)
    }

    fun setup(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch { accommodationEventHandler() }
        scope.launch { attractionEventHandler() }
        scope.launch { commuteEventHandler() }
        scope.launch { travelOfferEventHandler() }
        log.info("EntityEventHandler setup completed")
    }

    /**
     * Handles all AccommodationEvents by saving them using the AccommodationRepository
     */
    private suspend fun accommodationEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        EventBus.subscribe<AccommodationEvent> { event ->
            scope.launch {
                log.debug("Handling AccommodationEvent: ${event::class.simpleName}")
                try {
                    accommodationRepository.save(event)
                    log.debug("Successfully saved AccommodationEvent: ${event::class.simpleName}")
                } catch (e: Exception) {
                    log.error("Error saving AccommodationEvent: ${event::class.simpleName}", e)
                }
                }
        }
    }

    /**
     * Handles all AttractionEvents by saving them using the AttractionRepository
     */
    private suspend fun attractionEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        EventBus.subscribe<AttractionEvent> { event ->
            scope.launch {
                log.debug("Handling AttractionEvent: ${event::class.simpleName}")
                try {
                    attractionRepository.save(event)
                    log.debug("Successfully saved AttractionEvent: ${event::class.simpleName}")
                } catch (e: Exception) {
                    log.error("Error saving AttractionEvent: ${event::class.simpleName}", e)
                }
            }
        }
    }

    /**
     * Handles all CommuteEvents by saving them using the CommuteRepository
     */
    private suspend fun commuteEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        EventBus.subscribe<CommuteEvent> { event ->
            scope.launch {
                log.debug("Handling CommuteEvent: ${event::class.simpleName}")
                try {
                    commuteRepository.save(event)
                    log.debug("Successfully saved CommuteEvent: ${event::class.simpleName}")
                } catch (e: Exception) {
                    log.error("Error saving CommuteEvent: ${event::class.simpleName}", e)
                }
            }
        }
    }

    /**
     * Handles all TravelOfferEvents by saving them using the TravelOfferRepository
     */
    private suspend fun travelOfferEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        EventBus.subscribe<TravelOfferEvent> { event ->
            scope.launch {
                log.debug("Handling TravelOfferEvent: ${event::class.simpleName}")
                try {
                    travelOfferRepository.save(event)
                    log.debug("Successfully saved TravelOfferEvent: ${event::class.simpleName}")
                } catch (e: Exception) {
                    log.error("Error saving TravelOfferEvent: ${event::class.simpleName}", e)
                }
            }
        }
    }
}