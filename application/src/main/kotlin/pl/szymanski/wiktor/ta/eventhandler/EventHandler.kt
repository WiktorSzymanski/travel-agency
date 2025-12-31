package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus

abstract class EventHandler(
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val handlerJobs = mutableListOf<Job>()

    protected fun setupHandlers() {
        getHandlers().forEach { handlerFunction ->
            handlerJobs.add(scope.launch {
                handlerFunction()
            })
        }
    }

    abstract fun getHandlers(): List<suspend () -> Unit>
}