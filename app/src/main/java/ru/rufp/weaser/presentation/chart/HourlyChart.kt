package ru.rufp.weaser.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.rufp.weaser.R
import ru.rufp.weaser.data.repository.CachedHour
import ru.rufp.weaser.util.Format
import java.time.Instant

@Composable
fun HourlyChart(hours: List<CachedHour>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val bandColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val chartDescription = stringResource(R.string.cd_chart_temperature)
    val n = hours.size
    val labelStep = (n / 4).coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .semantics { contentDescription = chartDescription }
        ) {
            if (hours.isEmpty()) return@Canvas
            val stepX = size.width / hours.size

            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
            }

            hours.forEachIndexed { i, h ->
                val barHeight = size.height * h.probability.toFloat()
                drawRect(
                    color = barColor,
                    topLeft = Offset(i * stepX + 1f, size.height - barHeight),
                    size = Size(stepX - 2f, barHeight)
                )
                if (h.maxProbability > h.minProbability && h.maxProbability > 0.0) {
                    val top = size.height * h.maxProbability.toFloat()
                    val bottom = size.height * h.minProbability.toFloat()
                    drawRect(
                        color = bandColor,
                        topLeft = Offset(i * stepX + 1f, size.height - top),
                        size = Size(stepX - 2f, (top - bottom).coerceAtLeast(1f))
                    )
                }
            }

            val temps = hours.mapNotNull { it.temperature }
            if (temps.size >= 2) {
                val minT = temps.min()
                val maxT = temps.max()
                val range = (maxT - minT).takeIf { it > 0.01 } ?: 1.0
                val mapped = hours.mapIndexed { i, h ->
                    val t = h.temperature ?: minT
                    val yNorm = 1.0 - (t - minT) / range
                    val y = (0.08 + 0.84 * yNorm).toFloat() * size.height
                    Offset(i * stepX + stepX / 2f, y)
                }
                val path = Path().apply {
                    moveTo(mapped.first().x, mapped.first().y)
                    for (p in mapped.drop(1)) lineTo(p.x, p.y)
                }
                drawPath(path, color = lineColor, style = Stroke(width = 3f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            hours.forEachIndexed { i, h ->
                if (i % labelStep == 0 || i == n - 1) {
                    Spacer(Modifier.weight(1f))
                    Text(Format.hhmm(Instant.ofEpochMilli(h.timeEpochMillis)), style = MaterialTheme.typography.labelSmall)
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
