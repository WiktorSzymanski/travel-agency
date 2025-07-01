package scheduler

import generator.GeneratorLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

open class DataScheduler(
    private val generatorLoader: GeneratorLoader,
    protected val coroutineContext: CoroutineContext = Dispatchers.Default,
) {
    private var job: Job? = null
    protected val generators = generatorLoader.getGenerators()

    fun start() {
        if (job != null) {
            return // Already running
        }

        job =
            CoroutineScope(coroutineContext).launch {
                while (isActive) {
                    generate()
                    delay(generatorLoader.getInterval() * 1000)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun generate() {
        generators.forEach { it.process() }
    }
}
