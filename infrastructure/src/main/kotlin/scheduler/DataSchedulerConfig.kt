package scheduler

data class DataSchedulerConfig(
    val intervalSeconds: Long,
    val generators: List<GeneratorConfig>,
)

data class GeneratorConfig(
    val type: GeneratorType,
    val yamlFilePath: String,
    val futureDurationSeconds: Long,
)

enum class GeneratorType {
    ACCOMMODATION,
    ATTRACTION,
    COMMUTE,
}
