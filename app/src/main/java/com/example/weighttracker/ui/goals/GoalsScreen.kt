package com.example.weighttracker.ui.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.weighttracker.domain.model.WeightGoal
import com.example.weighttracker.domain.model.WeightUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val KG_TO_LB = 2.2046226218

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    goals: List<WeightGoal>,
    unit: WeightUnit,
    currentWeightKg: Double?,
    onAddGoal: (Double, WeightUnit, LocalDate) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Weight Goals") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { innerPadding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No goals set. Tap + to add a goal.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    goals.sortedByDescending { it.targetDate },
                    key = { it.id }
                ) { goal ->
                    GoalCard(
                        goal = goal,
                        unit = unit,
                        onDelete = { onDeleteGoal(goal.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            unit = unit,
            currentWeightKg = currentWeightKg,
            onDismiss = { showAddDialog = false },
            onConfirm = { weight, targetUnit, date ->
                onAddGoal(weight, targetUnit, date)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GoalCard(
    goal: WeightGoal,
    unit: WeightUnit,
    onDelete: () -> Unit
) {
    val weight = when (unit) {
        WeightUnit.Kilograms -> goal.roundedKg()
        WeightUnit.Pounds -> goal.roundedLbs()
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = goal.targetDate.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$weight ${unit.symbol}",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Goal"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    unit: WeightUnit,
    currentWeightKg: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, WeightUnit, LocalDate) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(70)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var changeText by remember { mutableStateOf("-10") }
    var weeksText by remember { mutableStateOf("10") }

    val currentInUnit = currentWeightKg?.let {
        when (unit) {
            WeightUnit.Kilograms -> it
            WeightUnit.Pounds -> it * KG_TO_LB
        }
    }
    val target = weightText.toDoubleOrNull()
    val delta = if (target != null && currentInUnit != null) target - currentInUnit else null
    val daysToTarget = ChronoUnit.DAYS.between(LocalDate.now(), selectedDate)
    val pacePerWeek = if (delta != null && daysToTarget > 0) delta / (daysToTarget / 7.0) else null

    val applyPreset: (Double, Int) -> Unit = { changeAmount, weeks ->
        if (currentInUnit != null) {
            weightText = "%.1f".format(currentInUnit + changeAmount)
            selectedDate = LocalDate.now().plusDays(weeks * 7L)
            changeText = formatChange(changeAmount)
            weeksText = weeks.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "New goal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HeroWeightField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    unit = unit,
                    delta = delta
                )

                DateRow(
                    selectedDate = selectedDate,
                    onClick = { showDatePicker = true }
                )

                AnimatedVisibility(
                    visible = pacePerWeek != null && delta != null && abs(delta) > 0.05,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (pacePerWeek != null) {
                        PaceRow(pacePerWeek = pacePerWeek, unit = unit)
                    }
                }

                if (currentInUnit != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    CustomPresetRow(
                        changeText = changeText,
                        onChangeTextChange = { changeText = it },
                        weeksText = weeksText,
                        onWeeksTextChange = { weeksText = it },
                        unit = unit,
                        onApply = {
                            val change = changeText.toDoubleOrNull() ?: return@CustomPresetRow
                            val weeks = weeksText.toIntOrNull() ?: return@CustomPresetRow
                            if (weeks > 0) applyPreset(change, weeks)
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { target?.let { onConfirm(it, unit, selectedDate) } },
                        enabled = target != null && target > 0
                    ) {
                        Text("Save goal")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun HeroWeightField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: WeightUnit,
    delta: Double?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TARGET WEIGHT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("\\d*\\.?\\d*"))) {
                        onValueChange(input)
                    }
                },
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.widthIn(min = 96.dp)
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = unit.symbol,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        AnimatedVisibility(
            visible = delta != null && abs(delta) > 0.05,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (delta != null) {
                val isLoss = delta < 0
                val color = if (isLoss) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                val arrow = if (isLoss) "↓" else "↑"
                Text(
                    text = "$arrow ${"%.1f".format(abs(delta))} ${unit.symbol} from current",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DateRow(
    selectedDate: LocalDate,
    onClick: () -> Unit
) {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), selectedDate)
    val relative = when {
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        days < 0L -> "${-days} days ago"
        days < 14L -> "$days days from today"
        days % 7L == 0L -> "${days / 7} weeks from today"
        else -> "$days days from today"
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "TARGET DATE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = relative,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedDate.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaceRow(pacePerWeek: Double, unit: WeightUnit) {
    val isLoss = pacePerWeek < 0
    val color = if (isLoss) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "PACE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${"%.2f".format(abs(pacePerWeek))} ${unit.symbol} / week",
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

@Composable
private fun CustomPresetRow(
    changeText: String,
    onChangeTextChange: (String) -> Unit,
    weeksText: String,
    onWeeksTextChange: (String) -> Unit,
    unit: WeightUnit,
    onApply: () -> Unit
) {
    val canApply = changeText.toDoubleOrNull() != null &&
        weeksText.toIntOrNull()?.let { it > 0 } == true

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Set by rate",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = changeText,
                onValueChange = onChangeTextChange,
                label = { Text("Change") },
                suffix = { Text(unit.symbol) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weeksText,
                onValueChange = onWeeksTextChange,
                label = { Text("Weeks") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        TextButton(
            onClick = onApply,
            enabled = canApply,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Apply")
        }
    }
}

private fun formatChange(change: Double): String =
    if (change == change.toInt().toDouble()) change.toInt().toString()
    else "%.1f".format(change)
