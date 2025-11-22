package pl.szymanski.wiktor.ta.saga

class Saga<T> {
    private val steps: MutableList<Pair<suspend (T) -> T, suspend (T) -> Unit>> = mutableListOf()

    fun addStep(
        operation: suspend (T) -> T,
        compensation: suspend (T) -> Unit,
    ): Saga<T> {
        steps.add(Pair(operation, compensation))
        return this
    }

    suspend fun process(context: T): Result<T> {
        val executedSteps = mutableListOf<Pair<suspend (T) -> T, suspend (T) -> Unit>>()
        var currentContext = context

        for (step in steps) {
            try {
                currentContext = step.first(currentContext)
                executedSteps.add(step)
            } catch (e: Exception) {
                runCompensation(currentContext, executedSteps)
                return Result.failure(e)
            }
        }

        return Result.success(currentContext)
    }

    private suspend fun runCompensation(
        context: T,
        executedSteps: List<Pair<suspend (T) -> T, suspend (T) -> Unit>>,
    ) {
        // Compensate all executed steps in reverse order
        for (step in executedSteps.reversed()) {
            try {
                step.second(context)
            } catch (e: Exception) {
                // Log compensation failure if needed
            }
        }
    }
}
