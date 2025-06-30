package generator

import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class YamlConfigLoaderTest {

    val loader = YamlConfigLoader()

    @Test
    fun `load commutes yaml config`() {
        val templates: List<CommuteTemplate> = loader.loadConfig("src/test/resources/commutes.yaml")

        val expected = listOf(
            CommuteTemplate(
                "London to Paris",
                "London",
                "Paris",
                listOf(
                    Seat("A", "1"),
                    Seat("A", "2"),
                )
            ),
            CommuteTemplate(
                "Paris to Berlin",
                "Paris",
                "Berlin",
                listOf(
                    Seat("B", "3"),
                    Seat("B", "4"),
                )
            )
        )

        assertEquals(expected, templates)
    }

    @Test
    fun `load attractions yaml config`() {
        val templates: List<AttractionTemplate> = loader.loadConfig("src/test/resources/attractions.yaml")

        val expected = listOf(
            AttractionTemplate(
                "Big Ben tour",
                "London",
                10
            ),
            AttractionTemplate(
                "City tour by boat",
                "Berlin",
                30
            )
        )

        assertEquals(expected, templates)
    }

    @Test
    fun `load accommodations yaml config`() {
        val templates: List<AccommodationTemplate> = loader.loadConfig("src/test/resources/accommodations.yaml")

        val expected = listOf(
            AccommodationTemplate(
                "Hotel",
                "London",
                Rent(
                    LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                    LocalDateTime.of(2025, 1, 5, 0, 0, 0),
                )
            ),
            AccommodationTemplate(
                "Hostel",
                "Berlin",
                Rent(
                    LocalDateTime.of(2025, 1, 3, 0, 0, 0),
                    LocalDateTime.of(2025, 1, 6, 0, 0, 0),
                )
            )
        )

        assertEquals(expected, templates)
    }

    @Test
    fun `handle invalid yaml`() {
        assertFailsWith<Exception> {
            loader.loadConfig<CommuteTemplate>("src/test/resources/attraction.yaml")
        }
    }
}