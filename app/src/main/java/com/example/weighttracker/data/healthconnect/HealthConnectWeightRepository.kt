package com.example.weighttracker.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
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
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class HealthConnectWeightRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeightRepository {

    private val healthConnectClient: HealthConnectClient = HealthConnectClient.getOrCreate(context)
    private val migrationPrefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
    private val weights = MutableStateFlow<List<WeightEntry>>(emptyList())

    override val weightStream: Flow<List<WeightEntry>> = weights.asStateFlow()

    override suspend fun refresh(start: Instant?, end: Instant?) {
        withContext(dispatcher) {
            runCatching { migrateRecordingMethodIfNeeded() }
                .onFailure { Log.w("WeightTracker", "Recording-method migration failed; will retry", it) }

            val timeRange = buildTimeRange(start, end)
            val allRecords = readAllRecords(timeRange)

            weights.value = allRecords
                .sortedBy { it.time }
                .map { it.toDomain() }
        }
    }

    private suspend fun readAllRecords(timeRange: TimeRangeFilter?): List<WeightRecord> {
        val all = mutableListOf<WeightRecord>()
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
            all += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return all
    }

    private suspend fun migrateRecordingMethodIfNeeded() {
        if (migrationPrefs.getBoolean(MIGRATION_FLAG, false)) return

        val ourPackage = context.packageName
        val candidates = readAllRecords(timeRange = null).filter {
            it.metadata.dataOrigin.packageName == ourPackage &&
                it.metadata.recordingMethod != Metadata.RECORDING_METHOD_MANUAL_ENTRY
        }

        if (candidates.isNotEmpty()) {
            val rebuilt = candidates.map { old ->
                WeightRecord(
                    weight = old.weight,
                    time = old.time,
                    zoneOffset = old.zoneOffset,
                    metadata = manualEntryMetadata()
                )
            }
            healthConnectClient.insertRecords(rebuilt)
            healthConnectClient.deleteRecords(
                WeightRecord::class,
                candidates.map { it.metadata.id },
                emptyList()
            )
            Log.d("WeightTracker", "Re-tagged ${candidates.size} weight records with MANUAL_ENTRY")
        }

        migrationPrefs.edit().putBoolean(MIGRATION_FLAG, true).apply()
    }

    private fun manualEntryMetadata(): Metadata = Metadata(
        clientRecordId = UUID.randomUUID().toString(),
        recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY
    )

    override suspend fun addWeight(value: Double, unit: WeightUnit, recordedAt: Instant) {
        withContext(dispatcher) {
            val weightInKg = when (unit) {
                WeightUnit.Kilograms -> value
                WeightUnit.Pounds -> value / POUNDS_PER_KG
            }

            val record = WeightRecord(
                weight = Mass.kilograms(weightInKg),
                time = recordedAt,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(recordedAt),
                metadata = manualEntryMetadata()
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
        private const val MIGRATION_PREFS = "weight_tracker_migrations"
        private const val MIGRATION_FLAG = "migrated_recording_method_v1"
    }
}
