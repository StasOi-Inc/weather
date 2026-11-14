package ru.rufp.weaser.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.rufp.weaser.R
import ru.rufp.weaser.data.repository.SourceRow
import ru.rufp.weaser.domain.model.SourceStatus
import ru.rufp.weaser.util.Format
import java.time.Instant

@Composable
fun SourcesScreen(modifier: Modifier = Modifier, viewModel: SourcesViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val warmUp by viewModel.warmUp.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.reload() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.tab_sources), style = MaterialTheme.typography.headlineSmall)
        if (warmUp) {
            Text(
                stringResource(R.string.warmup_notice),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        rows.forEach { row -> SourceCard(row, onToggle = viewModel::toggle) }
    }
}

@Composable
private fun SourceCard(row: SourceRow, onToggle: (String, Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            row.requiresKey && !row.keyPresent -> stringResource(R.string.source_no_key)
                            !row.enabled -> stringResource(R.string.source_disabled)
                            row.status == SourceStatus.OK -> stringResource(R.string.source_status_ok)
                            row.status == SourceStatus.STALE -> stringResource(R.string.source_status_stale)
                            else -> stringResource(R.string.source_status_error)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (row.status == SourceStatus.ERROR || (row.requiresKey && !row.keyPresent))
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = row.enabled,
                    onCheckedChange = { onToggle(row.id, it) },
                    enabled = row.keyPresent
                )
            }

            val statusText = row.lastSuccessAt?.let { last ->
                stringResource(R.string.data_from, Format.hhmm(Instant.ofEpochMilli(last.toEpochMilli())))
            } ?: stringResource(R.string.source_never)
            Text(statusText, style = MaterialTheme.typography.labelSmall)

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                MetricCell(stringResource(R.string.column_weight), Format.probability(row.weight))
                MetricCell(
                    stringResource(R.string.column_brier),
                    if (row.brier.isNaN()) stringResource(R.string.accuracy_warmup) else "%.3f".format(row.brier)
                )
                MetricCell(
                    stringResource(R.string.column_mae),
                    if (row.maeTemperature.isNaN()) "—" else "%.1f°".format(row.maeTemperature)
                )
                MetricCell(
                    stringResource(R.string.column_hit_rate),
                    if (row.hitRate.isNaN()) "—" else Format.probability(row.hitRate)
                )
            }

            if (row.nextHours.isNotEmpty()) {
                Text(stringResource(R.string.column_forecast), style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    row.nextHours.take(12).forEach { (time, prob) ->
                        Text(
                            Format.hhmm(time) + " " + Format.probability(prob),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String) {
    Column(Modifier.padding(end = 20.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
