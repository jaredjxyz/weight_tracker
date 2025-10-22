package com.example.weighttracker.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.weighttracker.R
import com.example.weighttracker.data.healthconnect.HealthConnectPermissions
import com.example.weighttracker.health.launchHealthConnectInstall
import com.example.weighttracker.health.launchHealthConnectSettings
import com.example.weighttracker.ui.dashboard.DashboardScreen
import com.example.weighttracker.ui.log.LogScreen
import com.example.weighttracker.ui.theme.WeightTrackerTheme
import com.example.weighttracker.data.healthconnect.HealthConnectStatus

private enum class WeightTrackerDestination(
    val route: String,
    val labelRes: Int
) {
    Dashboard("dashboard", R.string.dashboard_title),
    Log("log", R.string.log_title)
}

@Composable
fun WeightTrackerApp(
    viewModel: WeightTrackerViewModel
) {
    WeightTrackerTheme {
        val context = LocalContext.current
        val navController = rememberNavController()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val installHealthConnect = remember(context) {
            { context.launchHealthConnectInstall() }
        }
        val openHealthConnectSettings = remember(context) {
            { context.launchHealthConnectSettings() }
        }

        var autoPrompted by rememberSaveable { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = viewModel.permissionRequestContract(),
            onResult = viewModel::onPermissionsResult
        )

        val handleGrantPermissions = remember(
            uiState.availability,
            permissionLauncher,
            installHealthConnect,
            openHealthConnectSettings
        ) {
            {
                Log.d("WeightTracker", "Grant button clicked, availability=${uiState.availability}")
                when (uiState.availability) {
                    HealthConnectStatus.NOT_INSTALLED -> installHealthConnect()
                    HealthConnectStatus.NOT_SUPPORTED -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_open_health_connect),
                            Toast.LENGTH_LONG
                        ).show()
                        openHealthConnectSettings()
                    }
                    else -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_requesting_permissions),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("WeightTracker", "Launching Health Connect permission launcher")
                        Log.d("WeightTracker", "Permissions being requested: ${HealthConnectPermissions.weightPermissions}")
                        permissionLauncher.launch(HealthConnectPermissions.weightPermissions)
                    }
                }
            }
        }

        LaunchedEffect(uiState.availability, uiState.permissionsGranted) {
            if (uiState.availability != HealthConnectStatus.INSTALLED) {
                autoPrompted = false
            }
            if (uiState.availability == HealthConnectStatus.INSTALLED &&
                uiState.permissionsGranted.not() &&
                autoPrompted.not()
            ) {
                autoPrompted = true
                Log.d("WeightTracker", "Auto prompting Health Connect permission request")
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_requesting_permissions),
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("WeightTracker", "Auto-launching Health Connect permission contract")
                Log.d("WeightTracker", "Permissions being requested: ${HealthConnectPermissions.weightPermissions}")
                permissionLauncher.launch(HealthConnectPermissions.weightPermissions)
            }
        }

        Scaffold(
            bottomBar = {
                WeightTrackerNavigationBar(navController = navController)
            }
        ) { paddingValues ->
            WeightTrackerNavHost(
                navController = navController,
                modifier = Modifier.padding(paddingValues),
                viewModel = viewModel,
                uiState = uiState,
                onRequestPermissions = handleGrantPermissions,
                onInstallHealthConnect = installHealthConnect
            )
        }
    }
}

@Composable
private fun WeightTrackerNavHost(
    navController: NavHostController,
    modifier: Modifier,
    viewModel: WeightTrackerViewModel,
    uiState: WeightTrackerUiState,
    onRequestPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = WeightTrackerDestination.Dashboard.route,
        modifier = modifier
    ) {
        composable(WeightTrackerDestination.Dashboard.route) {
            DashboardScreen(
                uiState = uiState,
                onRefresh = viewModel::refreshWeights,
                onGrantPermissions = onRequestPermissions,
                onInstallHealthConnect = onInstallHealthConnect
            )
        }
        composable(WeightTrackerDestination.Log.route) {
            LogScreen(
                uiState = uiState,
                onAddWeight = viewModel::addWeight,
                onDeleteWeight = viewModel::deleteWeight,
                onChangeUnit = viewModel::updatePreferredUnit,
                onGrantPermissions = onRequestPermissions,
                onInstallHealthConnect = onInstallHealthConnect
            )
        }
    }
}

@Composable
private fun WeightTrackerNavigationBar(
    navController: NavHostController
) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    NavigationBar {
        WeightTrackerDestination.values().forEach { destination ->
            val selected = destination.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    }
                },
                icon = {
                    when (destination) {
                        WeightTrackerDestination.Dashboard ->
                            Icon(
                                imageVector = Icons.Outlined.BarChart,
                                contentDescription = null
                            )

                        WeightTrackerDestination.Log ->
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = null
                            )
                    }
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes)
                    )
                }
            )
        }
    }
}
