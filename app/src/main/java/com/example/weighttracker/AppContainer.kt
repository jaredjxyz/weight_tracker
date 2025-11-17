package com.example.weighttracker

import android.content.Context
import com.example.weighttracker.data.healthconnect.HealthConnectAvailabilityChecker
import com.example.weighttracker.data.healthconnect.HealthConnectWeightRepository
import com.example.weighttracker.data.local.DataStoreGoalRepository
import com.example.weighttracker.domain.repository.WeightRepository
import com.example.weighttracker.domain.repository.GoalRepository
import com.example.weighttracker.domain.usecase.WeightTrendCalculator
import com.example.weighttracker.health.HealthConnectPermissionManager

class AppContainer(context: Context) {
    val weightRepository: WeightRepository = HealthConnectWeightRepository(context)
    val goalRepository: GoalRepository = DataStoreGoalRepository(context)
    val trendCalculator = WeightTrendCalculator()
    val availabilityChecker = HealthConnectAvailabilityChecker(context)
    val permissionManager = HealthConnectPermissionManager(context)
}
