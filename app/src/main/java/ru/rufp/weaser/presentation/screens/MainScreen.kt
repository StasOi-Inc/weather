package ru.rufp.weaser.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.rufp.weaser.R
import ru.rufp.weaser.data.repository.CachedHour
import ru.rufp.weaser.presentation.MainViewModel
import ru.rufp.weaser.presentation.chart.HourlyChart
import ru.rufp.weaser.util.Format
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val forecast = state.forecast

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        if (forecast == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.loading))
            }
            return@Column
        }

        val issued = Instant.ofEpochMilli(forecast.issuedAtEpochMillis)
        Text(
            stringResource(R.string.data_from, Format.hhmm(issued)),
            style = MaterialTheme.typography.labelMedium
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.now_title), style = MaterialTheme.typography.titleMedium)
                val current = forecast.hours.firstOrNull()
                if (current != null) {
                    Text(
                        Format.temperature(current.temperature) + " " +
                            Format.precipGlyph(current.type) + " " +
                            Format.precipTypeName(current.type, context),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(stringResource(R.string.feels_like, Format.temperature(current.apparentTemperature)))
                    Text(stringResource(R.string.wind, Format.wind(current.windSpeed)))
                    Text(stringResource(R.string.humidity, Format.humidity(current.humidity)))
                    Text(stringResource(R.string.pressure, Format.pressureMm(current.pressure)))
                    Text(
                        stringResource(R.string.intensity_mm_h, Format.intensity(current.intensity)) +
                            " • " + Format.probability(current.probability),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (forecast.intervalStart != null && forecast.intervalEnd != null && forecast.intervalType != null) {
            val start = forecast.intervalStart
            val end = forecast.intervalEnd
            val type = runCatching {
                ru.rufp.weaser.domain.model.PrecipType.valueOf(forecast.intervalType)
            }.getOrNull() ?: ru.rufp.weaser.domain.model.PrecipType.RAIN
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

        if (forecast.sourcesDisagree) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.sources_disagree),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    val minP = forecast.hours.minOf { it.minProbability }
                    val maxP = forecast.hours.maxOf { it.maxProbability }
                    Text(
                        stringResource(R.string.sources_disagree_range, Format.probability(minP), Format.probability(maxP)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text(stringResource(R.string.next_24_hours), style = MaterialTheme.typography.titleMedium)
        HourlyChart(forecast.hours.take(24), Modifier.fillMaxWidth())

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(forecast.hours.take(24)) { hour ->
                HourCard(hour, context)
            }
        }
    }
}

@Composable
private fun HourCard(hour: CachedHour, context: android.content.Context) {
    Card(Modifier.width(92.dp)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(hhmmOf(hour.timeEpochMillis), style = MaterialTheme.typography.labelMedium)
            Text(Format.temperature(hour.temperature), style = MaterialTheme.typography.titleMedium)
            Text(Format.probability(hour.probability), style = MaterialTheme.typography.bodySmall)
            Text(Format.precipGlyph(hour.type) + " " + Format.precipTypeName(hour.type, context),
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val hhmmFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private fun hhmmOf(epochMillis: Long): String =
    hhmmFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
