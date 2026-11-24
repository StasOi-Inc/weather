package ru.rufp.weather.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import ru.rufp.weather.R
import ru.rufp.weather.data.repository.GeoSearchResult
import ru.rufp.weather.presentation.MainViewModel

@OptIn(FlowPreview::class)
@Composable
fun LocationSheet(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    val queryFlow = remember { MutableStateFlow("") }
    val results = remember { mutableStateOf<List<GeoSearchResult>>(emptyList()) }
    val notFound = remember { mutableStateOf(false) }
    val permissionDenied = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        queryFlow
            .debounce(400)
            .collect { q ->
                if (q.length >= 2) {
                    val found = runCatching { viewModel.search(q) }.getOrDefault(emptyList())
                    results.value = found
                    notFound.value = found.isEmpty()
                } else {
                    results.value = emptyList()
                    notFound.value = false
                }
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            viewModel.useMyLocation { }
        } else {
            permissionDenied.value = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.locations)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        queryFlow.value = it
                    },
                    label = { Text(stringResource(R.string.search_city_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (notFound.value && query.length >= 2) {
                    Text(stringResource(R.string.geocoding_not_found), style = MaterialTheme.typography.bodySmall)
                }

                results.value.take(5).forEach { result ->
                    TextButton(
                        onClick = { viewModel.addLocation(result) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            result.name + (result.country.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.useMyLocation { }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Text(stringResource(R.string.use_my_location), modifier = Modifier.padding(start = 4.dp))
                }

                if (permissionDenied.value) {
                    Text(
                        stringResource(R.string.location_permission_denied),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = { viewModel.checkForUpdates() },
                    enabled = !state.updateChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.updateChecking) stringResource(R.string.update_checking)
                        else stringResource(R.string.update_check)
                    )
                }

                if (state.locations.isEmpty()) {
                    Text(stringResource(R.string.no_saved_locations), style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        items(state.locations, key = { it.id }) { location ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectLocation(location.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        (if (location.isCurrent) "● " else "") + location.name,
                                        color = if (location.isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteLocation(location.id) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete_location)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
