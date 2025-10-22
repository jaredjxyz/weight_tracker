package com.example.weighttracker.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthConnectAvailabilityChecker(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun availability(): HealthConnectStatus =
        withContext(dispatcher) {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE ->
                    HealthConnectStatus.INSTALLED
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    HealthConnectStatus.NOT_INSTALLED
                HealthConnectClient.SDK_UNAVAILABLE ->
                    HealthConnectStatus.NOT_SUPPORTED
                else -> HealthConnectStatus.NOT_SUPPORTED
            }
        }
}

enum class HealthConnectStatus {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}
