package com.example.weighttracker.domain.model

import androidx.annotation.StringRes
import com.example.weighttracker.R

enum class WeightUnit(@StringRes val labelRes: Int, val symbol: String) {
    Kilograms(R.string.unit_kilograms, "kg"),
    Pounds(R.string.unit_pounds, "lb");

    companion object {
        fun fromString(value: String?): WeightUnit =
            when (value?.lowercase()) {
                "lbs", "lb", "pounds" -> Pounds
                else -> Kilograms
            }
    }
}
