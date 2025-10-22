package com.example.weighttracker.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.example.weighttracker.domain.model.WeightEntry
import com.example.weighttracker.domain.model.WeightUnit
import com.example.weighttracker.domain.repository.WeightRepository
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class HealthConnectWeightRepository(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeightRepository {

    private val healthConnectClient: HealthConnectClient = HealthConnectClient.getOrCreate(context)
    private val weights = MutableStateFlow<List<WeightEntry>>(emptyList())

    override val weightStream: Flow<List<WeightEntry>> = weights.asStateFlow()

    override suspend fun refresh(start: Instant?, end: Instant?) {
        withContext(dispatcher) {
            val timeRange = buildTimeRange(start, end)
            val allRecords = mutableListOf<WeightRecord>()
            var pageToken: String? = null

            do {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = timeRange ?: TimeRangeFilter(),
                        pageToken = pageToken,
                        ascendingOrder = true
                    )
                )
                allRecords += response.records
                pageToken = response.pageToken
            } while (pageToken != null)

            weights.value = allRecords
                .sortedBy { it.time }
                .map { it.toDomain() }
        }
    }

    override suspend fun addWeight(value: Double, unit: WeightUnit, recordedAt: Instant) {
        withContext(dispatcher) {
            val weightInKg = when (unit) {
                WeightUnit.Kilograms -> value
                WeightUnit.Pounds -> value / POUNDS_PER_KG
            }

            val record = WeightRecord(
                weight = Mass.kilograms(weightInKg),
                time = recordedAt,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(recordedAt)
            )

            healthConnectClient.insertRecords(listOf(record))
            refresh()
        }
    }

    override suspend fun deleteWeight(id: String) {
        withContext(dispatcher) {
            try {
                healthConnectClient.deleteRecords(
                    WeightRecord::class,
                    listOf(id),
                    emptyList()
                )
            } catch (ioException: IOException) {
                throw ioException
            }
            refresh()
        }
    }

    private suspend fun refresh() {
        refresh(start = null, end = null)
    }

    private fun buildTimeRange(start: Instant?, end: Instant?): TimeRangeFilter? = when {
        start != null && end != null -> TimeRangeFilter.between(start, end)
        start != null -> TimeRangeFilter.after(start)
        end != null -> TimeRangeFilter.before(end)
        else -> null
    }

    private fun WeightRecord.toDomain(): WeightEntry {
        val entryZone = zoneOffset?.let { ZoneId.ofOffset("UTC", it) } ?: ZoneId.systemDefault()
        return WeightEntry(
            id = metadata.id,
            weightKg = weight.inKilograms,
            recordedAt = time,
            zoneId = entryZone
        )
    }

    private companion object {
        private const val POUNDS_PER_KG = 2.2046226218
    }
}
