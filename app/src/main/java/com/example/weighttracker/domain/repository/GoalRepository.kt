package com.example.weighttracker.domain.repository

import com.example.weighttracker.domain.model.WeightGoal
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing weight goals.
 */
interface GoalRepository {
    /**
     * Flow of all weight goals, ordered by target date (ascending)
     */
    val goalsStream: Flow<List<WeightGoal>>

    /**
     * Add a new weight goal
     */
    suspend fun addGoal(goal: WeightGoal)

    /**
     * Update an existing weight goal
     */
    suspend fun updateGoal(goal: WeightGoal)

    /**
     * Delete a weight goal by ID
     */
    suspend fun deleteGoal(id: String)

    /**
     * Get all goals
     */
    suspend fun getAllGoals(): List<WeightGoal>
}
