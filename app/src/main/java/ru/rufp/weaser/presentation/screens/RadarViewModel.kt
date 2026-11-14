package ru.rufp.weaser.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.rufp.weaser.data.remote.RainViewerApi
import ru.rufp.weaser.domain.model.SavedLocation
import ru.rufp.weaser.data.repository.LocationRepository
import javax.inject.Inject

data class RadarFrame(
    val timeSeconds: Long,
    val tileUrl: String,
    val isNowcast: Boolean
)

data class RadarUiState(
    val frames: List<RadarFrame> = emptyList(),
    val currentIndex: Int = 0,
    val playing: Boolean = false,
    val location: SavedLocation? = null,
    val error: Boolean = false
)

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
                val frames = (maps.radar.past.map { it to false } + maps.radar.nowcast.map { it to true })
                    .mapNotNull { (frame, nowcast) ->
                        if (frame.path.isBlank()) null
                        else RadarFrame(
                            timeSeconds = frame.time,
                            tileUrl = "$host${frame.path}/256/{z}/{x}/{y}/2/1_1.png",
                            isNowcast = nowcast
                        )
                    }
                _state.value = _state.value.copy(
                    frames = frames,
                    currentIndex = (maps.radar.past.size - 1).coerceIn(0, (frames.size - 1).coerceAtLeast(0)),
                    error = frames.isEmpty()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = true)
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
            _state.value = s.copy(currentIndex = 0)
            true
        } else {
            _state.value = s.copy(currentIndex = next)
            false
        }
    }
}
