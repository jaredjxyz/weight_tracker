package com.example.weighttracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weighttracker.R
import com.example.weighttracker.domain.model.TrendPoint
import com.example.weighttracker.domain.model.WeightEntry
import com.example.weighttracker.domain.model.WeightUnit
import com.example.weighttracker.ui.WeightTrackerUiState
import com.example.weighttracker.ui.components.EmptyState
import com.example.weighttracker.ui.components.InfoCallout
import com.example.weighttracker.ui.components.LoadingState
import com.example.weighttracker.ui.components.PermissionCallout
import com.example.weighttracker.data.healthconnect.HealthConnectStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: WeightTrackerUiState,
    onRefresh: () -> Unit,
    onGrantPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = uiState.permissionsGranted) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(id = R.string.action_refresh)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Content(
            uiState = uiState,
            onGrantPermissions = onGrantPermissions,
            onInstallHealthConnect = onInstallHealthConnect,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun Content(
    uiState: WeightTrackerUiState,
    onGrantPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState.availability) {
        HealthConnectStatus.NOT_INSTALLED ->
            PermissionCallout(
                title = stringResource(R.string.health_connect_not_installed_title),
                message = stringResource(R.string.health_connect_not_installed_message),
                buttonLabel = stringResource(id = R.string.action_install_health_connect),
                onActionClick = onInstallHealthConnect,
                modifier = modifier
            )

        HealthConnectStatus.NOT_SUPPORTED ->
            InfoCallout(
                message = stringResource(R.string.health_connect_not_supported),
                modifier = modifier
            )

        else -> {
            when {
                uiState.isLoading && uiState.entries.isEmpty() -> {
                    LoadingState(modifier = modifier)
                }

                uiState.permissionsGranted.not() -> {
                    PermissionCallout(
                        title = stringResource(R.string.health_connect_permission_title),
                        message = stringResource(R.string.health_connect_permission_message),
                        buttonLabel = stringResource(id = R.string.action_grant_permission),
                        onActionClick = onGrantPermissions,
                        modifier = modifier
                    )
                }

                uiState.entries.isEmpty() -> {
                    EmptyState(
                        message = stringResource(id = R.string.dashboard_empty_state),
                        modifier = modifier
                    )
                }

                else -> {
                    LoadedState(uiState = uiState, modifier = modifier)
                }
            }
        }
    }
}

@Composable
private fun LoadedState(
    uiState: WeightTrackerUiState,
    modifier: Modifier
) {
    val latest = uiState.entries.firstOrNull()
    val previous = uiState.entries.drop(1).firstOrNull()
    val change = remember(latest, previous) {
        latest?.let { latestEntry ->
            previous?.let { previousEntry ->
                (latestEntry.weightKg - previousEntry.weightKg)
            }
        }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TrendCard(
                dailyPoints = uiState.trend.dailyPoints,
                rollingPoints = uiState.trend.rollingAveragePoints,
                latest = latest,
                unit = uiState.preferredUnit
            )
        }
        if (latest != null) {
            item {
                StatsCard(
                    latest = latest,
                    change = change,
                    unit = uiState.preferredUnit
                )
            }
        }
    }
}

@Composable
private fun TrendCard(
    dailyPoints: List<TrendPoint>,
    rollingPoints: List<TrendPoint>,
    latest: WeightEntry?,
    unit: WeightUnit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(id = R.string.dashboard_trend_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            TrendChart(
                dailyPoints = dailyPoints,
                rollingPoints = rollingPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            latest?.let {
                Text(
                    text = stringResource(
                        id = R.string.dashboard_latest_weight,
                        latest.rounded(unit),
                        unitLabel(unit)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    latest: WeightEntry,
    change: Double?,
    unit: WeightUnit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_stats_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(
                    id = R.string.dashboard_last_logged,
                    latest.displayDate,
                    latest.displayTime
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            change?.let {
                val trendText = stringResource(
                    id = if (it > 0) R.string.dashboard_weight_up else R.string.dashboard_weight_down,
                    kotlin.math.abs(it),
                    unitLabel(unit)
                )
                Text(
                    text = trendText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun WeightEntry.rounded(unit: WeightUnit): Double = when (unit) {
    WeightUnit.Kilograms -> roundedKg()
    WeightUnit.Pounds -> roundedLbs()
}

private fun unitLabel(unit: WeightUnit): String = unit.symbol
