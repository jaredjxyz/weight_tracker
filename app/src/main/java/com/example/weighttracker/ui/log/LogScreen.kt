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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
    onGrantPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    shouldFocusInput: Boolean = false,
    onFocusConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.log_title)) }
            )
        }
    ) { innerPadding ->
        LogContent(
            uiState = uiState,
            onAddWeight = onAddWeight,
            onDeleteWeight = onDeleteWeight,
            onGrantPermissions = onGrantPermissions,
            onInstallHealthConnect = onInstallHealthConnect,
            shouldFocusInput = shouldFocusInput,
            onFocusConsumed = onFocusConsumed,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun LogContent(
    uiState: WeightTrackerUiState,
    onAddWeight: (Double, WeightUnit, Instant) -> Unit,
    onDeleteWeight: (String) -> Unit,
    onGrantPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    shouldFocusInput: Boolean = false,
    onFocusConsumed: () -> Unit = {},
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
                    onAddWeight = onAddWeight,
                    onDeleteWeight = onDeleteWeight,
                    shouldFocusInput = shouldFocusInput,
                    onFocusConsumed = onFocusConsumed,
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
    onAddWeight: (Double, WeightUnit, Instant) -> Unit,
    onDeleteWeight: (String) -> Unit,
    shouldFocusInput: Boolean = false,
    onFocusConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var quickEntryValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(shouldFocusInput) {
        if (shouldFocusInput) {
            focusRequester.requestFocus()
            onFocusConsumed()
        }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickEntryField(
                value = quickEntryValue,
                onValueChange = { quickEntryValue = it },
                unit = unit,
                onSubmit = { weight ->
                    onAddWeight(weight, unit, Instant.now())
                    quickEntryValue = ""
                },
                focusRequester = focusRequester,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

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
private fun QuickEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: WeightUnit,
    onSubmit: (Double) -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Enter weight") },
                suffix = { Text(unit.symbol) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        value.toDoubleOrNull()?.let { weight ->
                            onSubmit(weight)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )

            IconButton(
                onClick = {
                    value.toDoubleOrNull()?.let { weight ->
                        onSubmit(weight)
                    }
                },
                enabled = value.toDoubleOrNull() != null
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.action_add_weight),
                    tint = if (value.toDoubleOrNull() != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
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
