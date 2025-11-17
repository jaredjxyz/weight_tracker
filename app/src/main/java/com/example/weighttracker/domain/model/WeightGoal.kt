package com.example.weighttracker.domain.model

import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Represents a weight goal with a target weight and target date.
 * Weight is always stored in kilograms internally, following the pattern of WeightEntry.
 */
data class WeightGoal(
    val id: String = UUID.randomUUID().toString(),
    val targetWeightKg: Double,
    val targetDate: LocalDate,
    val createdAt: LocalDate = LocalDate.now()
) {
    companion object {
        private const val KG_TO_LB = 2.2046226218
    }

    /**
     * Get the target weight in pounds
     */
    fun targetWeightLbs(): Double = targetWeightKg * KG_TO_LB

    /**
     * Get the rounded target weight in kilograms (to 1 decimal place)
     */
    fun roundedKg(): Double = (targetWeightKg * 10).roundToInt() / 10.0

    /**
     * Get the rounded target weight in pounds (to 1 decimal place)
     */
    fun roundedLbs(): Double = (targetWeightLbs() * 10).roundToInt() / 10.0

    /**
     * Check if the goal date is in the past
     */
    fun isPastDue(): Boolean = targetDate.isBefore(LocalDate.now())

    /**
     * Check if the goal date is today
     */
    fun isDueToday(): Boolean = targetDate.isEqual(LocalDate.now())

    /**
     * Get days remaining until target date (negative if past due)
     */
    fun daysRemaining(): Long =
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), targetDate)
}
