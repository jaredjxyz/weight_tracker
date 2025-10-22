package com.example.weighttracker.domain.repository

import com.example.weighttracker.domain.model.WeightEntry
import com.example.weighttracker.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface WeightRepository {
    val weightStream: Flow<List<WeightEntry>>

    suspend fun refresh(start: Instant? = null, end: Instant? = null)

    suspend fun addWeight(
        value: Double,
        unit: WeightUnit,
        recordedAt: Instant = Instant.now()
    )

    suspend fun deleteWeight(id: String)
}
