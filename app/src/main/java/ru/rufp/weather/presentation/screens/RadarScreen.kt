package ru.rufp.weather.presentation.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import ru.rufp.weather.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val BASE_STYLE_URI = "https://tiles.openfreemap.org/styles/liberty"
private const val RADAR_SOURCE_ID = "Weather-radar"
private const val RADAR_LAYER_ID = "Weather-radar-layer"
private const val MARKER_SOURCE_ID = "Weather-marker"
private const val MARKER_LAYER_ID = "Weather-marker-layer"
private const val TILESET_VERSION = "2.2.0"

@Composable
fun RadarScreen(modifier: Modifier = Modifier, viewModel: RadarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mapDescription = stringResource(R.string.cd_radar_map)

    LaunchedEffect(state.playing) {
        while (state.playing) {
            viewModel.advance()
            delay(700)
        }
    }

    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            val lp = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutParams = lp
        }
    }
    val mapReady = remember { androidx.compose.runtime.mutableStateOf(false) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.radar_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        if (state.error) {
            Text(
                stringResource(R.string.radar_no_frames),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        AndroidView(
            factory = { _ ->
                mapView.apply {
                    getMapAsync { map ->
                        map.setStyle(org.maplibre.android.maps.Style.Builder().fromUri(BASE_STYLE_URI)) { _ ->
                            mapReady.value = true
                        }
                    }
                }
            },
            update = { _ -> },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { contentDescription = mapDescription }
        )

        LaunchedEffect(mapReady.value, state.location, state.frames, state.currentIndex) {
            if (!mapReady.value) return@LaunchedEffect
            mapView.getMapAsync { map ->
                val style = map.style ?: return@getMapAsync
                val location = state.location
                if (location != null) {
                    if (style.getSource(MARKER_SOURCE_ID) == null) {
                        style.addSource(
                            GeoJsonSource(
                                MARKER_SOURCE_ID,
                                Feature.fromGeometry(
                                    Point.fromLngLat(location.longitude, location.latitude)
                                )
                            )
                        )
                        style.addLayer(
                            CircleLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID).withProperties(
                                circleColor("#1A73E8"),
                                circleRadius(8f),
                                circleOpacity(0.8f)
                            )
                        )
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(6.5)
                            .build()
                    } else {
                        (style.getSource(MARKER_SOURCE_ID) as? GeoJsonSource)
                            ?.setGeoJson(
                                Feature.fromGeometry(
                                    Point.fromLngLat(location.longitude, location.latitude)
                                )
                            )
                    }
                }
                val frames = state.frames
                if (frames.isNotEmpty()) {
                    val tileUrl = frames[state.currentIndex.coerceIn(0, frames.size - 1)].tileUrl
                    style.removeLayer(RADAR_LAYER_ID)
                    style.removeSource(RADAR_SOURCE_ID)
                    style.addSource(
                        RasterSource(RADAR_SOURCE_ID, TileSet(TILESET_VERSION, tileUrl))
                    )
                    style.addLayer(
                        RasterLayer(RADAR_LAYER_ID, RADAR_SOURCE_ID).withProperties(rasterOpacity(0.75f))
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.togglePlay() },
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.playing) stringResource(R.string.radar_pause)
                    else stringResource(R.string.radar_play)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Slider(
                    value = state.currentIndex.toFloat(),
                    onValueChange = { viewModel.selectIndex(it.toInt()) },
                    valueRange = 0f..(state.frames.size - 1).coerceAtLeast(0).toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                RadarLegend()
            }
            Column(horizontalAlignment = Alignment.End) {
                if (state.frames.getOrNull(state.currentIndex)?.isNowcast == true) {
                    Text(
                        text = stringResource(R.string.radar_forecast_chip),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = frameLabel(state),
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        state.frames.getOrNull(state.currentIndex)?.isNowcast == true ->
                            MaterialTheme.colorScheme.tertiary
                        state.currentIndex == state.nowIndex ->
                            MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun RadarLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .height(8.dp)
                .fillMaxWidth(0.6f)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFF66CC66),
                            androidx.compose.ui.graphics.Color(0xFFFFEE55),
                            androidx.compose.ui.graphics.Color(0xFFEE5533)
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                )
        )
        Text(stringResource(R.string.radar_legend_weak), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.radar_legend_strong), style = MaterialTheme.typography.labelSmall)
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

@Composable
private fun frameLabel(state: RadarUiState): String {
    if (state.frames.isEmpty()) return ""
    val frame = state.frames[state.currentIndex]
    val offsetMin = frame.offsetMinutes.toInt()
    val time = timeFormatter.format(Instant.ofEpochSecond(frame.timeSeconds).atZone(ZoneId.systemDefault()))
    return when {
        frame.isNowcast || offsetMin > 0 -> stringResource(R.string.radar_plus, offsetMin) + " " + time
        state.currentIndex == state.nowIndex -> stringResource(R.string.radar_now) + " " + time
        else -> stringResource(R.string.radar_minus, -offsetMin) + " " + time
    }
}
