package ru.rufp.weather.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.rufp.weather.data.repository.CachedForecast
import ru.rufp.weather.data.repository.ForecastCache
import ru.rufp.weather.data.repository.ForecastRepository
import ru.rufp.weather.data.repository.GeoSearchResult
import ru.rufp.weather.data.repository.LocationRepository
import ru.rufp.weather.domain.model.SavedLocation
import javax.inject.Inject

data class MainUiState(
    val loading: Boolean = false,
    val offline: Boolean = false,
    val error: Boolean = false,
    val forecast: CachedForecast? = null,
    val showLocationSheet: Boolean = false,
    val locations: List<SavedLocation> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val forecastRepository: ForecastRepository,
    private val locationRepository: LocationRepository,
    private val cache: ForecastCache
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state

    val cachedForecast: StateFlow<CachedForecast?> =
        cache.flow().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            locationRepository.ensureDefaultLocation()
            locationRepository.locations().collect { locations ->
                _state.value = _state.value.copy(locations = locations)
            }
        }
        viewModelScope.launch {
            cachedForecast.collect { cached ->
                if (cached != null && !_state.value.loading) {
                    _state.value = _state.value.copy(forecast = cached, offline = false, error = false)
                }
            }
        }
        viewModelScope.launch { refresh() }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = false)
            val hadCache = cache.cached() != null
            val ok = runCatching { forecastRepository.sync() }.getOrDefault(false)
            val cached = cache.cached()
            _state.value = _state.value.copy(
                loading = false,
                error = !ok && cached == null,
                offline = !ok && hadCache,
                forecast = cached ?: _state.value.forecast
            )
        }
    }

    fun openLocationSheet() {
        _state.value = _state.value.copy(showLocationSheet = true)
    }

    fun closeLocationSheet() {
        _state.value = _state.value.copy(showLocationSheet = false)
    }

    fun selectLocation(id: Long) {
        viewModelScope.launch {
            locationRepository.setCurrent(id)
            closeLocationSheet()
            refresh()
        }
    }

    suspend fun search(query: String): List<GeoSearchResult> =
        runCatching { locationRepository.search(query) }.getOrDefault(emptyList())

    fun addLocation(result: GeoSearchResult) {
        viewModelScope.launch {
            locationRepository.addLocation(result)
            closeLocationSheet()
            refresh()
        }
    }

    fun useMyLocation(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = locationRepository.addCurrentLocation()
            onResult(ok)
            if (ok) {
                closeLocationSheet()
                refresh()
            }
        }
    }

    fun deleteLocation(id: Long) {
        viewModelScope.launch {
            locationRepository.delete(id)
        }
    }
}
