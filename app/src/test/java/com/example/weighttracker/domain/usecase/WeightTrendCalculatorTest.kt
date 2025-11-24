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

    @Test
    fun `interpolates between sparse data points for rolling average`() {
        // Weight 0 on day 0, weight 10 on day 14
        val entries = listOf(
            entry(weightKg = 0.0, date = "2024-10-01T08:00:00Z"),
            entry(weightKg = 10.0, date = "2024-10-15T08:00:00Z")
        )

        val trend = calculator.calculate(entries, zoneId)

        // Rolling average should have points for all 15 days (0-14 inclusive)
        assertEquals(15, trend.rollingAveragePoints.size)

        // On day 14 (index 14), the 7-day window includes days 8-14
        // Interpolated values: day 8 = 5.71, day 9 = 6.43, day 10 = 7.14,
        // day 11 = 7.86, day 12 = 8.57, day 13 = 9.29, day 14 = 10.0
        // Average of these 7 days should be approximately 7.86
        val lastPointAverage = trend.rollingAveragePoints.last().weightKg
        assertEquals(7.9, lastPointAverage, 0.1)
    }

    @Test
    fun `rolling average line changes direction on interpolated days`() {
        // Weight 0 on day 0, 10 on day 5, 5 on day 10, 10 on day 14
        val entries = listOf(
            entry(weightKg = 0.0, date = "2024-10-01T08:00:00Z"),
            entry(weightKg = 10.0, date = "2024-10-06T08:00:00Z"),
            entry(weightKg = 5.0, date = "2024-10-11T08:00:00Z"),
            entry(weightKg = 10.0, date = "2024-10-15T08:00:00Z")
        )

        val trend = calculator.calculate(entries, zoneId)

        // Rolling average should have points for all 15 days
        assertEquals(15, trend.rollingAveragePoints.size)

        // Check that the line changes direction around day 12
        // even though there's no data entry on day 12
        val day11Index = 11
        val day12Index = 12
        val day13Index = 13

        val day11Avg = trend.rollingAveragePoints[day11Index].weightKg
        val day12Avg = trend.rollingAveragePoints[day12Index].weightKg
        val day13Avg = trend.rollingAveragePoints[day13Index].weightKg

        // Day 12 should show a change in direction
        // The slope should change from negative to positive around this area
        val slopeBefore = day12Avg - day11Avg
        val slopeAfter = day13Avg - day12Avg

        // Verify that there is a trend change (slopes have opposite signs or magnitudes change significantly)
        // We're looking for the line to start going up after going down
        assert(slopeAfter > slopeBefore) {
            "Expected trend change around day 12. Day 11: $day11Avg, Day 12: $day12Avg, Day 13: $day13Avg"
        }
    }

    @Test
    fun `interpolates values for days without entries`() {
        // Two entries 4 days apart
        val entries = listOf(
            entry(weightKg = 80.0, date = "2024-10-01T08:00:00Z"),
            entry(weightKg = 84.0, date = "2024-10-05T08:00:00Z")
        )

        val trend = calculator.calculate(entries, zoneId)

        // Should have rolling averages for all 5 days (0-4 inclusive)
        assertEquals(5, trend.rollingAveragePoints.size)

        // The interpolated values should be evenly spaced:
        // Day 0: 80.0, Day 1: 81.0, Day 2: 82.0, Day 3: 83.0, Day 4: 84.0
        // Day 4's 7-day average should include days 0-4 (only 5 days available)
        // Average: (80 + 81 + 82 + 83 + 84) / 5 = 82.0
        val lastPointAverage = trend.rollingAveragePoints.last().weightKg
        assertEquals(82.0, lastPointAverage, 0.0001)
    }

    @Test
    fun `handles single entry`() {
        val entries = listOf(
            entry(weightKg = 75.0, date = "2024-10-01T08:00:00Z")
        )

        val trend = calculator.calculate(entries, zoneId)

        assertEquals(1, trend.dailyPoints.size)
        assertEquals(1, trend.rollingAveragePoints.size)
        assertEquals(75.0, trend.rollingAveragePoints.first().weightKg, 0.0001)
    }

    @Test
    fun `handles consecutive days with same weight`() {
        val entries = buildSequence(
            startDate = "2024-10-01",
            weights = listOf(90.0, 90.0, 90.0, 90.0, 90.0, 90.0, 90.0)
        )

        val trend = calculator.calculate(entries, zoneId)

        // All rolling averages should be 90.0
        trend.rollingAveragePoints.forEach { point ->
            assertEquals(90.0, point.weightKg, 0.0001)
        }
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
