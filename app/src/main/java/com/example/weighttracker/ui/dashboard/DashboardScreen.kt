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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weighttracker.R
import com.example.weighttracker.domain.model.TrendPoint
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
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.dashboard_title)) }
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
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TrendCard(
                dailyPoints = uiState.trend.dailyPoints,
                rollingPoints = uiState.trend.rollingAveragePoints,
                unit = uiState.preferredUnit
            )
        }
    }
}

@Composable
private fun TrendCard(
    dailyPoints: List<TrendPoint>,
    rollingPoints: List<TrendPoint>,
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
                unit = unit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun unitLabel(unit: WeightUnit): String = unit.symbol
