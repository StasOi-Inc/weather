package ru.rufp.weather.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.rufp.weather.R
import ru.rufp.weather.data.repository.CachedForecast
import ru.rufp.weather.data.repository.CachedHour
import ru.rufp.weather.domain.model.PrecipType
import ru.rufp.weather.presentation.MainViewModel
import ru.rufp.weather.presentation.chart.HourlyChart
import ru.rufp.weather.presentation.icons.PrecipIcon
import ru.rufp.weather.util.Format
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface UpdateEntryPoint {
    fun installer(): ru.rufp.weather.update.UpdateInstaller
}

@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val forecast = state.forecast
    val installer = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext, UpdateEntryPoint::class.java
        ).installer()
    }
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle(null)
    var showUpdateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = forecast?.locationName ?: stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            AssistChip(
                onClick = { viewModel.openLocationSheet() },
                label = { Text(stringResource(R.string.locations)) }
            )
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = { viewModel.refresh() },
                label = {
                    if (state.loading) CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp))
                    else Text(stringResource(R.string.refresh))
                }
            )
        }

        if (state.offline && forecast != null) {
            Text(stringResource(R.string.offline_cache), style = MaterialTheme.typography.labelMedium)
        }
        if (state.error) {
            Text(stringResource(R.string.error_no_sources), color = MaterialTheme.colorScheme.error)
        }
        if (state.updateUpToDate) {
            Text(stringResource(R.string.update_up_to_date), style = MaterialTheme.typography.labelMedium)
        }
        if (state.updateFailed) {
            Text(
                stringResource(R.string.update_failed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (forecast == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.loading))
            }
            return@Column
        }

        val issued = Instant.ofEpochMilli(forecast.issuedAtEpochMillis)
        Text(
            stringResource(R.string.data_from, Format.hhmm(issued)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )

        availableUpdate?.let { update ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.update_available, update.versionName),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            stringResource(R.string.update_size, "%.1f".format(update.sizeBytes / 1_000_000.0)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    TextButton(onClick = { showUpdateDialog = true }) {
                        Text(stringResource(R.string.update_download))
                    }
                }
            }
        }

        HeroBlock(forecast, context)

        ConditionTilesRow(forecast)

        Text(stringResource(R.string.next_24_hours), style = MaterialTheme.typography.titleMedium)
        HourlyChart(forecast.hours.take(24), Modifier.fillMaxWidth())

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(forecast.hours.take(24)) { hour ->
                HourColumn(hour, context)
            }
        }

        if (forecast.sourcesDisagree) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.sources_disagree),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    val minP = forecast.hours.minOf { it.minProbability }
                    val maxP = forecast.hours.maxOf { it.maxProbability }
                    Text(
                        stringResource(R.string.sources_disagree_range, Format.probability(minP), Format.probability(maxP)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }

    if (showUpdateDialog) {
        availableUpdate?.let { update ->
            UpdateDialog(
                update = update,
                viewModel = viewModel,
                installer = installer,
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}

@Composable
private fun HeroBlock(forecast: CachedForecast, context: android.content.Context) {
    val current = forecast.hours.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (current != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Format.temperature(current.temperature),
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    PrecipIcon(
                        type = current.type,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        Format.precipTypeName(current.type, context),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.feels_like, Format.temperature(current.apparentTemperature)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(
                stringResource(R.string.intensity_mm_h, Format.intensity(current.intensity)) +
                    " • " + Format.probability(current.probability),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (forecast.intervalStart != null && forecast.intervalEnd != null && forecast.intervalType != null) {
            val start = forecast.intervalStart
            val end = forecast.intervalEnd
            val type = runCatching {
                PrecipType.valueOf(forecast.intervalType)
            }.getOrNull() ?: PrecipType.RAIN
            Text(
                stringResource(
                    R.string.rain_expected_from_to,
                    Format.precipTypeName(type, context),
                    Format.hhmm(Instant.ofEpochMilli(start)),
                    Format.hhmm(Instant.ofEpochMilli(end))
                ) + " " + stringResource(
                    R.string.rain_peak_about,
                    Format.hhmm(Instant.ofEpochMilli(forecast.intervalPeakTime ?: start)),
                    Format.probability(forecast.intervalPeakProbability ?: 0.0)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(stringResource(R.string.no_precipitation_expected), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ConditionTilesRow(forecast: CachedForecast) {
    val current = forecast.hours.firstOrNull() ?: return
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ConditionTile(stringResource(R.string.wind_label), Format.wind(current.windSpeed), Modifier.weight(1f))
        ConditionTile(stringResource(R.string.humidity_label), Format.humidity(current.humidity), Modifier.weight(1f))
        ConditionTile(stringResource(R.string.pressure_label), Format.pressureMm(current.pressure), Modifier.weight(1f))
        ConditionTile(stringResource(R.string.intensity_label), Format.intensity(current.intensity), Modifier.weight(1f))
    }
}

@Composable
private fun ConditionTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HourColumn(hour: CachedHour, context: android.content.Context) {
    Card(
        Modifier.width(92.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(hhmmOf(hour.timeEpochMillis), style = MaterialTheme.typography.labelMedium)
            PrecipIcon(type = hour.type, modifier = Modifier.size(22.dp))
            Text(Format.temperature(hour.temperature), style = MaterialTheme.typography.titleMedium)
            ProbabilityBar(hour.probability)
            Text(Format.probability(hour.probability), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProbabilityBar(probability: Double) {
    val color = when {
        probability > 0.8 -> MaterialTheme.colorScheme.error
        probability > 0.6 -> MaterialTheme.colorScheme.primary
        probability > 0.3 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        Modifier
            .width(28.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(probability.toFloat().coerceIn(0.02f, 1f))
                .background(color, RoundedCornerShape(6.dp))
        )
    }
}

private val hhmmFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private fun hhmmOf(epochMillis: Long): String =
    hhmmFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
