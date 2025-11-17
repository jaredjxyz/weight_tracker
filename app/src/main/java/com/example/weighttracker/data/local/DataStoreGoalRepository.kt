package com.example.weighttracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.weighttracker.domain.model.WeightGoal
import com.example.weighttracker.domain.repository.GoalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Implementation of GoalRepository using DataStore for local persistence.
 * Goals are stored as JSON strings in preferences.
 */
class DataStoreGoalRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : GoalRepository {

    companion object {
        private val Context.goalsDataStore: DataStore<Preferences> by preferencesDataStore(name = "weight_goals")
        private val GOALS_KEY = stringPreferencesKey("goals")
        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * Serializable version of WeightGoal for DataStore storage
     */
    @Serializable
    private data class SerializableGoal(
        val id: String,
        val targetWeightKg: Double,
        val targetDate: String, // ISO-8601 format
        val createdAt: String // ISO-8601 format
    )

    override val goalsStream: Flow<List<WeightGoal>> =
        context.goalsDataStore.data.map { preferences ->
            val goalsJson = preferences[GOALS_KEY] ?: "[]"
            val serializableGoals = json.decodeFromString<List<SerializableGoal>>(goalsJson)
            serializableGoals
                .map { it.toDomain() }
                .sortedBy { it.targetDate }
        }

    override suspend fun addGoal(goal: WeightGoal): Unit = withContext(dispatcher) {
        context.goalsDataStore.edit { preferences ->
            val currentGoals = getCurrentGoalsList(preferences)
            val updatedGoals = currentGoals + goal
            saveGoalsList(preferences, updatedGoals)
        }
    }

    override suspend fun updateGoal(goal: WeightGoal): Unit = withContext(dispatcher) {
        context.goalsDataStore.edit { preferences ->
            val currentGoals = getCurrentGoalsList(preferences)
            val updatedGoals = currentGoals.map { if (it.id == goal.id) goal else it }
            saveGoalsList(preferences, updatedGoals)
        }
    }

    override suspend fun deleteGoal(id: String): Unit = withContext(dispatcher) {
        context.goalsDataStore.edit { preferences ->
            val currentGoals = getCurrentGoalsList(preferences)
            val updatedGoals = currentGoals.filterNot { it.id == id }
            saveGoalsList(preferences, updatedGoals)
        }
    }

    override suspend fun getAllGoals(): List<WeightGoal> = withContext(dispatcher) {
        val preferences = context.goalsDataStore.data.map { it }.first()
        getCurrentGoalsList(preferences)
    }

    private fun getCurrentGoalsList(preferences: Preferences): List<WeightGoal> {
        val goalsJson = preferences[GOALS_KEY] ?: "[]"
        val serializableGoals = json.decodeFromString<List<SerializableGoal>>(goalsJson)
        return serializableGoals.map { it.toDomain() }
    }

    private fun saveGoalsList(preferences: MutablePreferences, goals: List<WeightGoal>) {
        val serializableGoals = goals.map { it.toSerializable() }
        val goalsJson = json.encodeToString(serializableGoals)
        preferences[GOALS_KEY] = goalsJson
    }

    private fun SerializableGoal.toDomain(): WeightGoal =
        WeightGoal(
            id = id,
            targetWeightKg = targetWeightKg,
            targetDate = LocalDate.parse(targetDate),
            createdAt = LocalDate.parse(createdAt)
        )

    private fun WeightGoal.toSerializable(): SerializableGoal =
        SerializableGoal(
            id = id,
            targetWeightKg = targetWeightKg,
            targetDate = targetDate.toString(),
            createdAt = createdAt.toString()
        )

    private suspend fun <T> Flow<T>.first(): T {
        var result: T? = null
        collect { value ->
            result = value
            return@collect
        }
        return result ?: throw NoSuchElementException("Flow was empty")
    }
}
