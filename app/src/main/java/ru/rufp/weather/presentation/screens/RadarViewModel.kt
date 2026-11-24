package ru.rufp.weather.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.rufp.weather.data.remote.RainViewerApi
import ru.rufp.weather.domain.model.SavedLocation
import ru.rufp.weather.data.repository.LocationRepository
import javax.inject.Inject

data class RadarFrame(
    val timeSeconds: Long,
    val tileUrl: String,
    val isNowcast: Boolean,
    val offsetMinutes: Long
)

data class RadarUiState(
    val frames: List<RadarFrame> = emptyList(),
    val currentIndex: Int = 0,
    val playing: Boolean = false,
    val location: SavedLocation? = null,
    val error: Boolean = false
) {
    val nowIndex: Int
        get() = frames.indexOfLast { !it.isNowcast }.coerceAtLeast(0)
}

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val rainViewerApi: RainViewerApi,
    locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RadarUiState())
    val state: StateFlow<RadarUiState> = _state

    init {
        viewModelScope.launch {
            locationRepository.currentLocation().collect { loc ->
                _state.value = _state.value.copy(location = loc)
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val maps = rainViewerApi.weatherMaps()
                val host = maps.host.ifBlank { "https://tilecache.rainviewer.com" }
                val past = maps.radar.past.filter { it.path.isNotBlank() }
                val referenceTime = past.lastOrNull()?.time
                    ?: maps.radar.nowcast.lastOrNull()?.time
                    ?: (System.currentTimeMillis() / 1000)
                val frames = (past.map { it to false } + maps.radar.nowcast.map { it to true })
                    .mapNotNull { (frame, nowcast) ->
                        if (frame.path.isBlank()) null
                        else RadarFrame(
                            timeSeconds = frame.time,
                            tileUrl = "$host${frame.path}/256/{z}/{x}/{y}/2/1_1.png",
                            isNowcast = nowcast,
                            offsetMinutes = (frame.time - referenceTime) / 60
                        )
                    }
                    .filter { it.offsetMinutes >= -60 }
                val nowIndex = frames.indexOfLast { !it.isNowcast }.coerceAtLeast(0)
                _state.value = RadarUiState(
                    frames = frames,
                    currentIndex = nowIndex,
                    playing = false,
                    error = frames.isEmpty()
                )
            } catch (e: Exception) {
                _state.value = RadarUiState(error = true)
            }
        }
    }

    fun togglePlay() {
        _state.value = _state.value.copy(playing = !_state.value.playing)
    }

    fun selectIndex(index: Int) {
        if (_state.value.frames.isNotEmpty()) {
            _state.value = _state.value.copy(
                currentIndex = index.coerceIn(0, _state.value.frames.size - 1),
                playing = false
            )
        }
    }

    fun advance(): Boolean {
        val s = _state.value
        if (s.frames.isEmpty()) return false
        val next = s.currentIndex + 1
        return if (next >= s.frames.size) {
            _state.value = s.copy(currentIndex = s.nowIndex)
            true
        } else {
            _state.value = s.copy(currentIndex = next)
            false
        }
    }
}
