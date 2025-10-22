package com.example.weighttracker.domain.usecase

import com.example.weighttracker.domain.model.WeightEntry
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class WeightTrendCalculatorTest {

    private val calculator = WeightTrendCalculator()
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun `calculates daily averages`() {
        val entries = listOf(
            entry(weightKg = 90.0, date = "2024-10-01T08:00:00Z"),
            entry(weightKg = 91.0, date = "2024-10-01T20:00:00Z"),
            entry(weightKg = 89.5, date = "2024-10-02T08:00:00Z")
        )

        val trend = calculator.calculate(entries, zoneId)

        assertEquals(2, trend.dailyPoints.size)
        assertEquals(90.5, trend.dailyPoints[0].weightKg, 0.0001)
        assertEquals(89.5, trend.dailyPoints[1].weightKg, 0.0001)
    }

    @Test
    fun `provides rolling averages`() {
        val entries = buildSequence(
            startDate = "2024-09-25",
            weights = listOf(90.0, 89.5, 89.0, 88.5, 88.0, 87.5, 87.0, 86.5)
        )

        val trend = calculator.calculate(entries, zoneId)

        // First point uses only available day
        assertEquals(90.0, trend.rollingAveragePoints.first().weightKg, 0.0001)
        // Eighth point average of last 7 days (excluding oldest)
        assertEquals(88.0, trend.rollingAveragePoints.last().weightKg, 0.0001)
    }

    private fun entry(weightKg: Double, date: String): WeightEntry =
        WeightEntry(
            id = date,
            weightKg = weightKg,
            recordedAt = Instant.parse(date)
        )

    private fun buildSequence(
        startDate: String,
        weights: List<Double>
    ): List<WeightEntry> {
        val start = ZonedDateTime.parse("${startDate}T08:00:00Z")
        return weights.mapIndexed { index, weight ->
            WeightEntry(
                id = "$startDate-$index",
                weightKg = weight,
                recordedAt = start.plusDays(index.toLong()).toInstant()
            )
        }
    }
}
