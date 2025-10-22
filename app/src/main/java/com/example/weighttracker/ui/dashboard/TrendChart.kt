package com.example.weighttracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.weighttracker.domain.model.TrendPoint
import com.example.weighttracker.domain.model.WeightUnit
import java.time.format.DateTimeFormatter

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
    val firstDate = allPoints.firstOrNull()?.date?.format(dateFormatter) ?: ""
    val lastDate = allPoints.lastOrNull()?.date?.format(dateFormatter) ?: ""

    Column(modifier = modifier) {
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

        Row(modifier = Modifier.fillMaxWidth()) {
            // Y-axis space
            Spacer(modifier = Modifier.width(40.dp))

            // Chart canvas
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp)
                    .padding(horizontal = 4.dp)
            ) {
                val chartHeight = size.height
                val chartWidth = size.width
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

                drawLine(dailyPoints, colorScheme.primary, 4.dp.toPx())
                drawLine(rollingPoints, colorScheme.tertiary, 3.dp.toPx())
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
