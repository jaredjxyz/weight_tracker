package com.example.weighttracker.domain.model

import java.time.LocalDate

data class TrendPoint(
    val date: LocalDate,
    val weightKg: Double
)

data class WeightTrend(
    val dailyPoints: List<TrendPoint>,
    val rollingAveragePoints: List<TrendPoint>
)
