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

        val points = mutableListOf<TrendPoint>()
        val rollingWindowValues = ArrayDeque<Pair<LocalDate, Double>>()
        var sum = 0.0

        sortedDates.forEach { date ->
            val value = dailyValues.getValue(date)
            rollingWindowValues.addLast(date to value)
            sum += value

            while (rollingWindowValues.isNotEmpty() &&
                rollingWindowValues.first().first.isBefore(date.minusDays((rollingWindow - 1).toLong()))
            ) {
                val removed = rollingWindowValues.removeFirst()
                sum -= removed.second
            }

            val average = if (rollingWindowValues.isEmpty()) {
                value
            } else {
                (sum / rollingWindowValues.size).roundToTenths()
            }

            points.add(TrendPoint(date, average))
        }

        return points
    }

    private fun Double.roundToTenths(): Double = (this * 10).roundToInt() / 10.0

    companion object {
        private const val DEFAULT_WINDOW_DAYS = 7
    }
}
