package com.example.weighttracker.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Represents a single stored weight sample sourced from Health Connect.
 */
data class WeightEntry(
    val id: String,
    val weightKg: Double,
    val recordedAt: Instant,
    val zoneId: ZoneId = ZoneId.systemDefault()
) {
    val displayDate: String
        get() = ZonedDateTime.ofInstant(recordedAt, zoneId)
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

    val displayTime: String
        get() = ZonedDateTime.ofInstant(recordedAt, zoneId)
            .format(DateTimeFormatter.ofPattern("h:mm a"))

    fun weightLbs(): Double = weightKg * KG_TO_LB

    fun roundedKg(): Double = (weightKg * 10).roundToInt() / 10.0

    fun roundedLbs(): Double = (weightLbs() * 10).roundToInt() / 10.0

    companion object {
        private const val KG_TO_LB = 2.2046226218
    }
}
