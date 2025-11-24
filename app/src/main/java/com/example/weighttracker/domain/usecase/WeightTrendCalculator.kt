package com.example.weighttracker.domain.usecase

import com.example.weighttracker.domain.model.TrendPoint
import com.example.weighttracker.domain.model.WeightEntry
import com.example.weighttracker.domain.model.WeightTrend
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class WeightTrendCalculator(
    private val rollingWindow: Int = DEFAULT_WINDOW_DAYS
) {

    fun calculate(
        entries: List<WeightEntry>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeightTrend {
        if (entries.isEmpty()) {
            return WeightTrend(emptyList(), emptyList())
        }

        val dailyValues = entries
            .groupBy { LocalDate.ofInstant(it.recordedAt, zoneId) }
            .mapValues { (_, dayEntries) ->
                dayEntries.map { it.weightKg }.average()
            }

        val sortedDates = dailyValues.keys.sorted()
        val dailyPoints = sortedDates.map { date ->
            TrendPoint(date, dailyValues.getValue(date))
        }

        val rollingAveragePoints = buildRollingAverage(sortedDates, dailyValues)

        return WeightTrend(
            dailyPoints = dailyPoints,
            rollingAveragePoints = rollingAveragePoints
        )
    }

    private fun buildRollingAverage(
        sortedDates: List<LocalDate>,
        dailyValues: Map<LocalDate, Double>
    ): List<TrendPoint> {
        if (sortedDates.isEmpty()) return emptyList()

        // Create continuous date range from first to last entry
        val firstDate = sortedDates.first()
        val lastDate = sortedDates.last()
        val allDates = mutableListOf<LocalDate>()
        var currentDate = firstDate
        while (!currentDate.isAfter(lastDate)) {
            allDates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        // Interpolate weight values for all dates
        val interpolatedValues = interpolateWeights(allDates, sortedDates, dailyValues)

        // Calculate 7-day moving average for each date
        val points = mutableListOf<TrendPoint>()

        allDates.forEach { date ->
            // Get the date range for this 7-day window (inclusive of current day)
            val windowStart = date.minusDays((rollingWindow - 1).toLong())

            // Collect all interpolated values within the window
            val windowValues = allDates
                .filter { it >= windowStart && it <= date }
                .mapNotNull { interpolatedValues[it] }

            if (windowValues.isNotEmpty()) {
                val average = (windowValues.sum() / windowValues.size).roundToTenths()
                points.add(TrendPoint(date, average))
            }
        }

        return points
    }

    private fun interpolateWeights(
        allDates: List<LocalDate>,
        sortedDates: List<LocalDate>,
        dailyValues: Map<LocalDate, Double>
    ): Map<LocalDate, Double> {
        val result = mutableMapOf<LocalDate, Double>()

        // Add all actual data points
        sortedDates.forEach { date ->
            result[date] = dailyValues.getValue(date)
        }

        // Interpolate values for dates without entries
        allDates.forEach { date ->
            if (date !in result) {
                // Find the surrounding dates with actual values
                val beforeDate = sortedDates.lastOrNull { it.isBefore(date) }
                val afterDate = sortedDates.firstOrNull { it.isAfter(date) }

                when {
                    beforeDate != null && afterDate != null -> {
                        // Interpolate between the two values
                        val beforeValue = dailyValues.getValue(beforeDate)
                        val afterValue = dailyValues.getValue(afterDate)
                        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(beforeDate, afterDate).toDouble()
                        val daysFromBefore = java.time.temporal.ChronoUnit.DAYS.between(beforeDate, date).toDouble()
                        val ratio = daysFromBefore / totalDays
                        result[date] = beforeValue + (afterValue - beforeValue) * ratio
                    }
                    beforeDate != null -> {
                        // Use the last known value
                        result[date] = dailyValues.getValue(beforeDate)
                    }
                    afterDate != null -> {
                        // Use the next known value
                        result[date] = dailyValues.getValue(afterDate)
                    }
                }
            }
        }

        return result
    }

    private fun Double.roundToTenths(): Double = (this * 10).roundToInt() / 10.0

    companion object {
        private const val DEFAULT_WINDOW_DAYS = 7
    }
}
