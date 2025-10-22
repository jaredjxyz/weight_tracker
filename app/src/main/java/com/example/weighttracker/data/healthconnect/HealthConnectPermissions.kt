package com.example.weighttracker.data.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord

object HealthConnectPermissions {
    // Try using the SDK methods directly - they should generate the correct format
    // for the platform version
    val weightPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class)
    )

    // Log the actual permission strings for debugging
    init {
        val readPerm = HealthPermission.getReadPermission(WeightRecord::class)
        val writePerm = HealthPermission.getWritePermission(WeightRecord::class)
        android.util.Log.d("HealthConnectPermissions", "SDK Version: ${android.os.Build.VERSION.SDK_INT}")
        android.util.Log.d("HealthConnectPermissions", "Read permission: $readPerm")
        android.util.Log.d("HealthConnectPermissions", "Write permission: $writePerm")
        android.util.Log.d("HealthConnectPermissions", "WeightRecord canonical name: ${WeightRecord::class.java.canonicalName}")
    }
}
