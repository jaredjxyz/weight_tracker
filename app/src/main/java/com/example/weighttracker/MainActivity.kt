package com.example.weighttracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import com.example.weighttracker.ui.WeightTrackerApp
import com.example.weighttracker.ui.WeightTrackerViewModel

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy {
        (application as WeightTrackerApplication).container
    }

    private val viewModel: WeightTrackerViewModel by viewModels {
        WeightTrackerViewModel.factory(
            repository = container.weightRepository,
            goalRepository = container.goalRepository,
            trendCalculator = container.trendCalculator,
            availabilityChecker = container.availabilityChecker,
            permissionManager = container.permissionManager
        )
    }

    private val widgetAddWeightRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            WeightTrackerApp(
                viewModel = viewModel,
                openAddWeightFromWidget = widgetAddWeightRequest.value,
                onWidgetActionConsumed = { widgetAddWeightRequest.value = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHealthConnectState()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_ADD_WEIGHT) {
            widgetAddWeightRequest.value = true
        }
    }

    companion object {
        const val ACTION_ADD_WEIGHT = "com.example.weighttracker.ADD_WEIGHT"
    }
}
