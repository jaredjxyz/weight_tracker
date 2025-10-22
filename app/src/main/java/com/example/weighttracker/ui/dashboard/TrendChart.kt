package com.example.weighttracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TrendChart(
    dailyPoints: List<TrendPoint>,
    rollingPoints: List<TrendPoint>,
    unit: WeightUnit,
    modifier: Modifier = Modifier
) {
    val pointCount = maxOf(dailyPoints.size, rollingPoints.size)
    if (pointCount < 2) {
        EmptyTrend(modifier)
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.labelSmall

    val combined = remember(dailyPoints, rollingPoints) {
        (dailyPoints + rollingPoints).distinctBy { it.date to it.weightKg }
    }
    val allPoints = dailyPoints.ifEmpty { rollingPoints }

    val minY = combined.minOf { it.weightKg }
    val maxY = combined.maxOf { it.weightKg }
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
    val firstDate = allPoints.firstOrNull()?.date?.format(dateFormatter) ?: ""
    val lastDate = allPoints.lastOrNull()?.date?.format(dateFormatter) ?: ""

    var selectedPoint by remember { mutableStateOf<TrendPoint?>(null) }
    var tapPosition by remember { mutableStateOf<Offset?>(null) }
    var chartWidth by remember { mutableStateOf(0f) }

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
                modifier = Modifier.padding(end = 16.dp)
            )
            LegendItem(
                color = colorScheme.tertiary,
                label = "7-day avg"
            )
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

                // Chart canvas
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .padding(horizontal = 4.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val chartWidth = size.width.toFloat()
                                val stepX = chartWidth / (pointCount - 1)

                                // Find closest point
                                var closestIndex = -1
                                var closestDistance = Float.MAX_VALUE

                                dailyPoints
                                    .forEachIndexed { index, _ ->
                                        val x = index * stepX
                                        val distance = abs(offset.x - x)
                                        if (distance < closestDistance && distance < 30.dp.toPx()) {
                                            closestDistance = distance
                                            closestIndex = index
                                        }
                                    }

                                if (closestIndex >= 0 && closestIndex < dailyPoints.size) {
                                    selectedPoint = dailyPoints[closestIndex]
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
                    val stepX = chartWidth / (pointCount - 1)

                    fun TrendPoint.toOffset(listIndex: Int): Offset {
                        val x = listIndex * stepX
                        val normalizedY = (weightKg - minY) / yRange
                        val y = chartHeight - (normalizedY * chartHeight)
                        return Offset(x, y.toFloat())
                    }

                    fun drawLine(points: List<TrendPoint>, color: Color, strokeWidth: Float) {
                        if (points.size < 2) return
                        val path = Path().apply {
                            val first = points.first().toOffset(0)
                            moveTo(first.x, first.y)
                            points.forEachIndexed { index, point ->
                                val offset = point.toOffset(index)
                                lineTo(offset.x, offset.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
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

                    drawLine(
                        start = Offset(0f, 0f),
                        end = Offset(0f, chartHeight),
                        color = colorScheme.outline,
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    // Draw data lines
                    drawLine(dailyPoints, colorScheme.primary, 4.dp.toPx())
                    drawLine(rollingPoints, colorScheme.tertiary, 3.dp.toPx())

                    // Draw selected point indicator
                    selectedPoint?.let { point ->
                        val index = dailyPoints.indexOf(point)
                        if (index >= 0) {
                            val offset = point.toOffset(index)

                            // Draw vertical line
                            drawLine(
                                start = Offset(offset.x, 0f),
                                end = Offset(offset.x, chartHeight),
                                color = colorScheme.primary.copy(alpha = 0.3f),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Draw circle at point
                            drawCircle(
                                color = colorScheme.primary,
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

            // Tooltip overlay
            selectedPoint?.let { point ->
                tapPosition?.let { position ->
                    val density = LocalDensity.current
                    val weightValue = when (unit) {
                        WeightUnit.Kilograms -> point.weightKg
                        WeightUnit.Pounds -> point.weightKg * 2.20462
                    }

                    // Smart positioning: place popup above point, or below if near top
                    val popupHeight = with(density) { 70.dp.toPx() } // Approximate popup height
                    val popupWidth = with(density) { 140.dp.toPx() } // Approximate popup width

                    val yOffset = if (position.y < popupHeight + 20) {
                        // Point is near top, place below
                        with(density) { (position.y + 20.dp.toPx()).roundToInt() }
                    } else {
                        // Place above the point
                        with(density) { (position.y - popupHeight - 10.dp.toPx()).roundToInt() }
                    }

                    // Center horizontally on the point, but keep within chart bounds
                    val xOffset = with(density) {
                        val centered = position.x - (popupWidth / 2) + 40.dp.toPx()
                        val minX = 40.dp.toPx()
                        val maxX = chartWidth + 40.dp.toPx() - popupWidth
                        centered.coerceIn(minX, maxX).roundToInt()
                    }

                    Popup(
                        alignment = Alignment.TopStart,
                        offset = IntOffset(x = xOffset, y = yOffset)
                    ) {
                        Card(
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp, 3.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
