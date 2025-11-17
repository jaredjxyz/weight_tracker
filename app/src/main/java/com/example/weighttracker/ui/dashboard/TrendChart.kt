package com.example.weighttracker.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.weighttracker.domain.model.TrendPoint
import com.example.weighttracker.domain.model.WeightUnit
import com.example.weighttracker.domain.model.WeightGoal
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TrendChart(
    dailyPoints: List<TrendPoint>,
    rollingPoints: List<TrendPoint>,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
    goals: List<WeightGoal> = emptyList()
) {
    // Convert goals to TrendPoints
    val goalPoints = remember(goals) {
        goals.map { goal ->
            TrendPoint(
                date = goal.targetDate,
                weightKg = goal.targetWeightKg
            )
        }.sortedBy { it.date }
    }

    // Calculate date range for positioning
    val dateRange = remember(dailyPoints, rollingPoints, goalPoints) {
        val allPoints = dailyPoints + rollingPoints + goalPoints
        if (allPoints.isEmpty()) return@remember null

        val minDate = allPoints.minOf { it.date }
        val maxDate = allPoints.maxOf { it.date }
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1

        Triple(minDate, maxDate, totalDays)
    }

    if (dateRange == null || dateRange.third < 2) {
        EmptyTrend(modifier)
        return
    }

    val (minDate, maxDate, totalDays) = dateRange

    // Create date-to-position mapping based on days from start
    val dateToPosition = remember(minDate, maxDate) {
        { date: java.time.LocalDate ->
            java.time.temporal.ChronoUnit.DAYS.between(minDate, date).toInt()
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.labelSmall

    // Display window constants
    val displayDays = 7
    val dayWidth = 60.dp // Width per day in the chart

    // Scroll state for horizontal scrolling
    val scrollState = rememberScrollState()

    // Calculate which data points are visible based on scroll position
    val density = LocalDensity.current
    val scrollOffset by remember { derivedStateOf { scrollState.value } }

    // Calculate visible window start/end indices
    val chartWidthPx by remember { derivedStateOf { displayDays * with(density) { dayWidth.toPx() } } }
    val totalWidthPx by remember { derivedStateOf { totalDays * with(density) { dayWidth.toPx() } } }

    var visibleStartIndex by remember { mutableStateOf(max(0, totalDays - displayDays)) }
    var visibleEndIndex by remember { mutableStateOf(totalDays) }

    // Update visible indices based on scroll
    LaunchedEffect(scrollOffset, totalDays) {
        val scrollFraction = if (totalWidthPx > chartWidthPx) {
            scrollOffset / (totalWidthPx - chartWidthPx)
        } else 0f

        visibleStartIndex = (scrollFraction * (totalDays - displayDays)).toInt().coerceIn(0, max(0, totalDays - displayDays))
        visibleEndIndex = (visibleStartIndex + displayDays).coerceAtMost(totalDays)
    }

    // Get visible data points based on actual date range
    val visibleStartDate = remember(minDate, visibleStartIndex) {
        minDate.plusDays(visibleStartIndex.toLong())
    }
    val visibleEndDate = remember(minDate, visibleEndIndex) {
        minDate.plusDays(visibleEndIndex.toLong())
    }

    val visibleDaily = remember(dailyPoints, visibleStartDate, visibleEndDate) {
        dailyPoints.filter { it.date >= visibleStartDate && it.date < visibleEndDate }
    }
    val visibleRolling = remember(rollingPoints, visibleStartDate, visibleEndDate) {
        rollingPoints.filter { it.date >= visibleStartDate && it.date < visibleEndDate }
    }

    // Calculate dynamic y-axis range based on visible data and goals
    val visibleCombined = remember(visibleDaily, visibleRolling) {
        (visibleDaily + visibleRolling).distinctBy { it.date to it.weightKg }
    }

    val targetMinY = remember(visibleCombined, goals) {
        val dataMin = visibleCombined.minOfOrNull { it.weightKg } ?: 0.0
        val goalMin = goals.minOfOrNull { it.targetWeightKg } ?: Double.MAX_VALUE
        minOf(dataMin, goalMin)
    }
    val targetMaxY = remember(visibleCombined, goals) {
        val dataMax = visibleCombined.maxOfOrNull { it.weightKg } ?: 100.0
        val goalMax = goals.maxOfOrNull { it.targetWeightKg } ?: Double.MIN_VALUE
        maxOf(dataMax, goalMax)
    }

    // Animated y-axis range for smooth transitions
    data class YAxisRange(val min: Double, val max: Double)

    val yRangeConverter = remember {
        TwoWayConverter<YAxisRange, AnimationVector2D>(
            convertToVector = { AnimationVector2D(it.min.toFloat(), it.max.toFloat()) },
            convertFromVector = { YAxisRange(it.v1.toDouble(), it.v2.toDouble()) }
        )
    }

    val animatedYRange = remember {
        Animatable(YAxisRange(targetMinY, targetMaxY), yRangeConverter)
    }

    LaunchedEffect(targetMinY, targetMaxY) {
        animatedYRange.animateTo(
            targetValue = YAxisRange(targetMinY, targetMaxY),
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 300f
            )
        )
    }

    val minY = animatedYRange.value.min
    val maxY = animatedYRange.value.max
    val yRange = (maxY - minY).takeIf { it != 0.0 } ?: 1.0

    val minWeight = when (unit) {
        WeightUnit.Kilograms -> minY
        WeightUnit.Pounds -> minY * 2.20462
    }
    val maxWeight = when (unit) {
        WeightUnit.Kilograms -> maxY
        WeightUnit.Pounds -> maxY * 2.20462
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val detailDateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val firstDate = visibleStartDate.format(dateFormatter)
    val lastDate = visibleEndDate.minusDays(1).format(dateFormatter)

    var selectedPoint by remember { mutableStateOf<TrendPoint?>(null) }
    var tapPosition by remember { mutableStateOf<Offset?>(null) }
    var chartWidth by remember { mutableStateOf(0f) }

    // Active line selection: 0=daily, 1=rolling, 2=goals
    var activeLine by remember { mutableStateOf(0) }

    // Scroll to show today on first composition
    LaunchedEffect(totalDays, totalWidthPx, chartWidthPx) {
        if (totalWidthPx > chartWidthPx && totalDays > displayDays) {
            val today = java.time.LocalDate.now()
            // Calculate days from minDate to today
            val daysToToday = java.time.temporal.ChronoUnit.DAYS.between(minDate, today).toInt()
            // Position today at the right edge of the visible window (subtract displayDays - 1)
            // because the last date shown is visibleEndDate - 1 day
            val targetStartIndex = (daysToToday - displayDays + 2).coerceIn(0, totalDays - displayDays)
            val scrollFraction = targetStartIndex.toFloat() / (totalDays - displayDays)
            val targetScrollPosition = (scrollFraction * (totalWidthPx - chartWidthPx)).toInt()
            scrollState.scrollTo(targetScrollPosition)
        }
    }

    Column(modifier = modifier) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                color = colorScheme.primary,
                label = "Daily",
                isActive = activeLine == 0,
                onClick = { activeLine = 0 },
                modifier = Modifier.padding(end = 16.dp)
            )
            LegendItem(
                color = colorScheme.tertiary,
                label = "7-day avg",
                isActive = activeLine == 1,
                onClick = { activeLine = 1 },
                modifier = if (goals.isNotEmpty()) Modifier.padding(end = 16.dp) else Modifier
            )
            if (goals.isNotEmpty()) {
                LegendItem(
                    color = colorScheme.secondary,
                    label = if (goals.size == 1) "Goal" else "Goals",
                    isActive = activeLine == 2,
                    onClick = { activeLine = 2 },
                    isDashed = true
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Y-axis label
            Text(
                text = "${maxWeight.toInt()} ${unit.symbol}",
                style = textStyle,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Box {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Y-axis space
                Spacer(modifier = Modifier.width(40.dp))

                // Scrollable chart container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .horizontalScroll(scrollState)
                ) {
                    // Chart canvas with fixed width based on total days
                    Canvas(
                        modifier = Modifier
                            .width(dayWidth * totalDays)
                            .height(200.dp)
                            .padding(horizontal = 4.dp)
                            .pointerInput(activeLine, visibleDaily, visibleRolling, goalPoints, minY, yRange, dateToPosition) {
                                detectTapGestures { offset ->
                                    val chartHeight = size.height
                                    val visiblePoints = when (activeLine) {
                                        0 -> visibleDaily
                                        1 -> visibleRolling
                                        2 -> goalPoints
                                        else -> visibleDaily
                                    }
                                    if (visiblePoints.isEmpty()) return@detectTapGestures

                                    val stepX = size.width / (totalDays - 1)

                                    // Find closest point on active line
                                    var closestPoint: TrendPoint? = null
                                    var closestDistance = Float.MAX_VALUE

                                    visiblePoints.forEach { point ->
                                        val index = dateToPosition(point.date)
                                        val x = index * stepX
                                        val normalizedY = (point.weightKg - minY) / yRange
                                        val y = chartHeight - (normalizedY * chartHeight).toFloat()

                                        // Calculate distance to this point
                                        val dx = offset.x - x
                                        val dy = offset.y - y
                                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                        if (distance < closestDistance && distance < 40.dp.toPx()) {
                                            closestDistance = distance
                                            closestPoint = point
                                        }
                                    }

                                    if (closestPoint != null) {
                                        selectedPoint = closestPoint
                                        tapPosition = offset
                                    } else {
                                        selectedPoint = null
                                        tapPosition = null
                                    }
                                }
                            }
                    ) {
                        val chartHeight = size.height
                        chartWidth = size.width
                        val stepX = chartWidth / (totalDays - 1)

                        fun TrendPoint.toOffset(): Offset {
                            val index = dateToPosition(date)
                            val x = index * stepX
                            val normalizedY = (weightKg - minY) / yRange
                            val y = chartHeight - (normalizedY * chartHeight)
                            return Offset(x, y.toFloat())
                        }

                        fun drawDataLine(points: List<TrendPoint>, color: Color, strokeWidth: Float, isDashed: Boolean = false) {
                            if (points.size < 2) return
                            val path = Path().apply {
                                val first = points.first().toOffset()
                                moveTo(first.x, first.y)
                                points.drop(1).forEach { point ->
                                    val offset = point.toOffset()
                                    lineTo(offset.x, offset.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                    pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(15f, 10f)) else null
                                )
                            )
                        }

                        // Draw axis lines
                        drawLine(
                            start = Offset(0f, chartHeight),
                            end = Offset(chartWidth, chartHeight),
                            color = colorScheme.outline,
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )

                        // Draw data lines (use full data sets, not visible filtered)
                        drawDataLine(dailyPoints, colorScheme.primary, 4.dp.toPx())
                        drawDataLine(rollingPoints, colorScheme.tertiary, 3.dp.toPx())

                        // Draw goal line as dashed line connecting goal points
                        if (goalPoints.size >= 2) {
                            drawDataLine(goalPoints, colorScheme.secondary, 2.dp.toPx(), isDashed = true)
                        } else if (goalPoints.size == 1) {
                            // Draw single goal point as a marker
                            val offset = goalPoints.first().toOffset()
                            drawCircle(
                                color = colorScheme.secondary,
                                radius = 6.dp.toPx(),
                                center = offset,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Draw selected point indicator
                        selectedPoint?.let { point ->
                            val (activePoints, activeColor) = when (activeLine) {
                                0 -> visibleDaily to colorScheme.primary
                                1 -> visibleRolling to colorScheme.tertiary
                                2 -> goalPoints to colorScheme.secondary
                                else -> visibleDaily to colorScheme.primary
                            }
                            if (activePoints.contains(point)) {
                                val offset = point.toOffset()

                                // Draw vertical line
                                drawLine(
                                    start = Offset(offset.x, 0f),
                                    end = Offset(offset.x, chartHeight),
                                    color = activeColor.copy(alpha = 0.3f),
                                    strokeWidth = 2.dp.toPx()
                                )

                                // Draw circle at point
                                drawCircle(
                                    color = activeColor,
                                    radius = 6.dp.toPx(),
                                    center = offset,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    color = colorScheme.surface,
                                    radius = 4.dp.toPx(),
                                    center = offset
                                )
                            }
                        }
                    }
                }
            }

            // Tooltip overlay
            selectedPoint?.let { point ->
                val weightValue = when (unit) {
                    WeightUnit.Kilograms -> point.weightKg
                    WeightUnit.Pounds -> point.weightKg * 2.20462
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = point.date.format(detailDateFormatter),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "%.1f %s".format(weightValue, unit.symbol),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(40.dp))
            Text(
                text = firstDate,
                style = textStyle,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = lastDate,
                style = textStyle,
                color = colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${minWeight.toInt()} ${unit.symbol}",
                style = textStyle,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDashed: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = if (isActive) {
        colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(
                if (!isActive) Modifier.clickable(onClick = onClick) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isDashed) {
            Canvas(modifier = Modifier.size(16.dp, 3.dp)) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp, 3.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyTrend(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "--",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
