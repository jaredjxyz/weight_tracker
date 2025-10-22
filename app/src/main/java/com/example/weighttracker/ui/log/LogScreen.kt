package com.example.weighttracker.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.weighttracker.R
import com.example.weighttracker.domain.model.WeightEntry
import com.example.weighttracker.domain.model.WeightUnit
import com.example.weighttracker.ui.WeightTrackerUiState
import com.example.weighttracker.ui.components.EmptyState
import com.example.weighttracker.ui.components.InfoCallout
import com.example.weighttracker.ui.components.LoadingState
import com.example.weighttracker.ui.components.PermissionCallout
import com.example.weighttracker.data.healthconnect.HealthConnectStatus
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    uiState: WeightTrackerUiState,
    onAddWeight: (Double, WeightUnit, Instant) -> Unit,
    onDeleteWeight: (String) -> Unit,
    onChangeUnit: (WeightUnit) -> Unit,
    onGrantPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.log_title)) },
                actions = {
                    IconButton(onClick = onGrantPermissions, enabled = uiState.permissionsGranted.not()) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(id = R.string.action_refresh)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.permissionsGranted) {
                FloatingActionButton(onClick = { dialogVisible = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.action_add_weight)
                    )
                }
            }
        }
    ) { innerPadding ->
        LogContent(
            uiState = uiState,
            onDeleteWeight = onDeleteWeight,
            onGrantPermissions = onGrantPermissions,
            onInstallHealthConnect = onInstallHealthConnect,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    if (dialogVisible) {
        AddWeightDialog(
            preferredUnit = uiState.preferredUnit,
            onConfirm = { value, unit ->
                onAddWeight(value, unit, Instant.now())
                onChangeUnit(unit)
                dialogVisible = false
            },
            onDismiss = { dialogVisible = false }
        )
    }
}

@Composable
private fun LogContent(
    uiState: WeightTrackerUiState,
    onDeleteWeight: (String) -> Unit,
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
                uiState.isLoading && uiState.entries.isEmpty() -> LoadingState(modifier = modifier)
                uiState.permissionsGranted.not() -> PermissionCallout(
                    title = stringResource(R.string.health_connect_permission_title),
                    message = stringResource(R.string.health_connect_permission_message),
                    buttonLabel = stringResource(id = R.string.action_grant_permission),
                    onActionClick = onGrantPermissions,
                    modifier = modifier
                )

                uiState.entries.isEmpty() -> EmptyState(
                    message = stringResource(R.string.log_empty_state),
                    modifier = modifier
                )

                else -> WeightList(
                    entries = uiState.entries,
                    unit = uiState.preferredUnit,
                    onDeleteWeight = onDeleteWeight,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun WeightList(
    entries: List<WeightEntry>,
    unit: WeightUnit,
    onDeleteWeight: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            WeightRow(
                entry = entry,
                unit = unit,
                onDeleteWeight = onDeleteWeight
            )
        }
    }
}

@Composable
private fun WeightRow(
    entry: WeightEntry,
    unit: WeightUnit,
    onDeleteWeight: (String) -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = entry.displayDate,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = entry.displayTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(
                        id = R.string.log_weight_value,
                        entry.rounded(unit),
                        unitLabel(unit)
                    ),
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(
                    onClick = { onDeleteWeight(entry.id) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete_entry)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWeightDialog(
    preferredUnit: WeightUnit,
    onConfirm: (Double, WeightUnit) -> Unit,
    onDismiss: () -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(preferredUnit) }
    val isConfirmEnabled = remember(weightInput) { weightInput.toDoubleOrNull() != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                weightInput.toDoubleOrNull()?.let { value ->
                    onConfirm(value, selectedUnit)
                }
            }, enabled = isConfirmEnabled) {
                Text(text = stringResource(id = R.string.action_save_weight))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        },
        title = { Text(text = stringResource(id = R.string.add_weight_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(text = stringResource(id = R.string.add_weight_field_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                WeightUnitSelector(
                    selectedUnit = selectedUnit,
                    onUnitSelected = { selectedUnit = it }
                )
            }
        }
    )
}

@Composable
private fun WeightUnitSelector(
    selectedUnit: WeightUnit,
    onUnitSelected: (WeightUnit) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WeightUnit.values().forEach { unit ->
            FilterChip(
                selected = selectedUnit == unit,
                onClick = { onUnitSelected(unit) },
                label = { Text(text = unitLabel(unit)) }
            )
        }
    }
}

private fun WeightEntry.rounded(unit: WeightUnit): Double = when (unit) {
    WeightUnit.Kilograms -> roundedKg()
    WeightUnit.Pounds -> roundedLbs()
}

private fun unitLabel(unit: WeightUnit): String = unit.symbol
