package ru.rufp.weather.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.rufp.weather.BuildConfig
import ru.rufp.weather.R
import ru.rufp.weather.data.repository.SourceRow
import ru.rufp.weather.util.Format
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AboutScreen(modifier: Modifier = Modifier, viewModel: AboutViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.tab_about), style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)))
                        }
                    }) { Text(stringResource(R.string.about_releases)) }
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL)))
                        }
                    }) { Text(stringResource(R.string.about_readme)) }
                }
                TextButton(
                    onClick = { viewModel.checkForUpdates() },
                    enabled = !state.checkingUpdates
                ) {
                    if (state.checkingUpdates) CircularProgressIndicator(
                        Modifier.height(16.dp),
                        strokeWidth = 2.dp
                    )
                    else Text(stringResource(R.string.update_check))
                }
                when {
                    state.updateUpToDate -> Text(
                        stringResource(R.string.update_up_to_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    state.updateFailed -> Text(
                        stringResource(R.string.update_failed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    state.latestTag != null -> Text(
                        stringResource(R.string.about_latest_version, state.latestTag!!),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.about_stats_title),
                    style = MaterialTheme.typography.titleMedium
                )
                if (state.warmUp) {
                    Text(
                        stringResource(R.string.warmup_notice),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                ObservationProgress(state.observationCount, state.warmUp)
                if (state.rows.isEmpty()) {
                    Text(
                        stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    state.rows.forEach { row -> SourceStatsBlock(row) }
                }
            }
        }
    }
}

@Composable
private fun ObservationProgress(count: Int, warmUp: Boolean) {
    val target = WARM_UP_TARGET
    val fraction = (count.toFloat() / target).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.about_observations, count, target),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            if (warmUp) {
                Text(
                    stringResource(R.string.about_warmup_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun SourceStatsBlock(row: SourceRow) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .height(10.dp)
                    .background(statusColor(row), CircleShape)
            )
            Text(row.displayName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.about_weight_value, Format.probability(row.weight)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        WeightBar(row.weight)
        MetricRow(
            label = stringResource(R.string.column_brier),
            value = if (row.brier.isNaN()) "—" else "%.3f".format(row.brier),
            fraction = if (row.brier.isNaN()) null else (row.brier / BRIER_MAX).coerceIn(0.0, 1.0),
            good = !row.brier.isNaN() && row.brier <= BRIER_GOOD
        )
        MetricRow(
            label = stringResource(R.string.column_mae),
            value = if (row.maeTemperature.isNaN()) "—" else "%.1f°".format(row.maeTemperature),
            fraction = if (row.maeTemperature.isNaN()) null else (row.maeTemperature / MAE_MAX).coerceIn(0.0, 1.0),
            good = !row.maeTemperature.isNaN() && row.maeTemperature <= MAE_GOOD
        )
        MetricRow(
            label = stringResource(R.string.column_hit_rate),
            value = if (row.hitRate.isNaN()) "—" else Format.probability(row.hitRate),
            fraction = if (row.hitRate.isNaN()) null else row.hitRate.coerceIn(0.0, 1.0),
            good = !row.hitRate.isNaN() && row.hitRate >= HIT_RATE_GOOD
        )
        row.lastSuccessAt?.let { last ->
            Text(
                stringResource(
                    R.string.data_from,
                    lastHourMinute(last.toEpochMilli())
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        } ?: Text(
            stringResource(R.string.source_never),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun WeightBar(weight: Double) {
    val fraction = weight.coerceIn(0.0, 1.0).toFloat()
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String, fraction: Double?, good: Boolean) {
    val barColor = when {
        fraction == null -> MaterialTheme.colorScheme.outlineVariant
        good -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelMedium)
        }
        MiniBar(
            fraction = fraction?.toFloat() ?: 0f,
            color = barColor,
            enabled = fraction != null
        )
    }
}

@Composable
private fun MiniBar(fraction: Float, color: Color, enabled: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
    ) {
        if (enabled) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun statusColor(row: SourceRow): Color = when {
    row.requiresKey && !row.keyPresent -> MaterialTheme.colorScheme.outlineVariant
    !row.enabled -> MaterialTheme.colorScheme.outlineVariant
    else -> MaterialTheme.colorScheme.primary
}

private fun lastHourMinute(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

private const val WARM_UP_TARGET = 20
private const val BRIER_MAX = 0.5
private const val BRIER_GOOD = 0.25
private const val MAE_MAX = 5.0
private const val MAE_GOOD = 2.0
private const val HIT_RATE_GOOD = 0.6
private const val RELEASES_URL = "https://github.com/StasOi-Inc/weather/releases"
private const val PROJECT_URL = "https://github.com/StasOi-Inc/weather#readme"
