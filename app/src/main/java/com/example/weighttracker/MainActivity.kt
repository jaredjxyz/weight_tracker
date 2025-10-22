package com.example.weighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.weighttracker.ui.WeightTrackerApp
import com.example.weighttracker.ui.WeightTrackerViewModel

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy {
        (application as WeightTrackerApplication).container
    }

    private val viewModel: WeightTrackerViewModel by viewModels {
        WeightTrackerViewModel.factory(
            repository = container.weightRepository,
            trendCalculator = container.trendCalculator,
            availabilityChecker = container.availabilityChecker,
            permissionManager = container.permissionManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WeightTrackerApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHealthConnectState()
    }
}
