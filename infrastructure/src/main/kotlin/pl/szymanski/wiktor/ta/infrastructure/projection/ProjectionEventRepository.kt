package pl.szymanski.wiktor.ta.infrastructure.projection

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.SubscribeToStreamOptions
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService
import java.util.concurrent.atomic.AtomicLong

class ProjectionEventRepository {
    companion object {
        private val log = LoggerFactory.getLogger(TravelOfferExpireService::class.java)
    }

    private val checkpoint = AtomicLong(-1L)

    fun getSubscriptionOptions(fromRevision: Long): SubscribeToStreamOptions = when(fromRevision) {
        -1L -> {
            SubscribeToStreamOptions.get()
                .fromStart()
                .resolveLinkTos()
        }
        else -> {
            SubscribeToStreamOptions.get()
                .fromRevision(checkpoint.get())
                .resolveLinkTos()
        }
    }

    fun subscribe(
        entityType: String,
        fromRevision: String?,
        updateProjection: suspend (Event, Int) -> Unit
    ) {
//        val scope = CoroutineScope(Dispatchers.Default)
//        checkpoint.set(fromRevision)
//
//        val subscriptionOptions = getSubscriptionOptions(fromRevision)
//
//        val listener = object : SubscriptionListener() {
//            override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
//                scope.launch {
//                    val eventTypeName = resolvedEvent.event.eventType
//                    val eventClass: Class<*> = Class.forName(eventTypeName)
//
//                    updateProjection(
//                        EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass) as Event,
//                        resolvedEvent.event.revision.toInt())
//
//                    checkpoint.set(resolvedEvent.originalEvent.revision)
//                }
//            }
//
//            override fun onCancelled(subscription: Subscription, exception: Throwable) {
//                CoroutineScope(Dispatchers.Default).launch {
//                    log.warn("Subscription for $entityType stream dropped: ${exception.message}")
//                    subscribe(entityType, checkpoint.get(), updateProjection)
//                }
//            }
//        }
//
//        log.info("Subscribing to $entityType stream from position: $checkpoint")
//        kurrentClient.subscribeToStream(
//            "\$ce-$entityType",
//            listener,
//            subscriptionOptions)
    }
}