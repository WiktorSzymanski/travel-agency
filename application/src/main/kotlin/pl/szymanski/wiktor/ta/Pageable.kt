package pl.szymanski.wiktor.ta

data class Pageable(
    val page: Int,
    val size: Int,
    val sort: Sort = Sort()
)

data class Page<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long
) {
    fun <R> map(transform: (T) -> R): Page<R> =
        Page(content.map(transform), page, size, totalElements)
}

data class Sort(
    val orders: List<Order> = emptyList()
) {
    data class Order(
        val property: String,
        val direction: Direction
    )

    enum class Direction {
        ASC, DESC
    }
}