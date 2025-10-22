package com.example.weighttracker.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.weighttracker.data.healthconnect.HealthConnectPermissions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthConnectPermissionManager(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val client: HealthConnectClient = HealthConnectClient.getOrCreate(context)
    private val permissionController = client.permissionController

    fun permissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    suspend fun getGrantedPermissions(): Set<String> =
        withContext(dispatcher) { permissionController.getGrantedPermissions() }

    suspend fun hasAllPermissions(): Boolean =
        withContext(dispatcher) {
            val granted = permissionController.getGrantedPermissions()
            granted.containsAll(HealthConnectPermissions.weightPermissions)
        }
}
