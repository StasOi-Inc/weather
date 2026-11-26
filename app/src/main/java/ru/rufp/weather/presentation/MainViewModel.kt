package ru.rufp.weather.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
import ru.rufp.weather.update.UpdateChecker
import ru.rufp.weather.update.UpdateDownloader
import ru.rufp.weather.update.UpdateInfo
import ru.rufp.weather.update.UpdateStorage
import javax.inject.Inject

data class MainUiState(
    val loading: Boolean = false,
    val offline: Boolean = false,
    val error: Boolean = false,
    val forecast: CachedForecast? = null,
    val showLocationSheet: Boolean = false,
    val locations: List<SavedLocation> = emptyList(),
    val update: UpdateInfo? = null,
    val updateChecking: Boolean = false,
    val updateUpToDate: Boolean = false,
    val updateFailed: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forecastRepository: ForecastRepository,
    private val locationRepository: LocationRepository,
    private val cache: ForecastCache,
    private val updateChecker: UpdateChecker,
    private val updateDownloader: UpdateDownloader,
    private val updateStorage: UpdateStorage
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state

    val cachedForecast: StateFlow<CachedForecast?> =
        cache.flow().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val availableUpdate: Flow<UpdateInfo?> = updateStorage.available()

    init {
        viewModelScope.launch { updateStorage.purgeOutdated() }
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
        viewModelScope.launch {
            updateStorage.available().collect { update ->
                _state.value = _state.value.copy(update = update)
            }
        }
        viewModelScope.launch { refresh() }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = false)
            val hadCache = cache.cached() != null
            val ok = try {
                forecastRepository.sync()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            val cached = cache.cached()
            _state.value = _state.value.copy(
                loading = false,
                error = !ok && cached == null,
                offline = !ok && hadCache,
                forecast = cached ?: _state.value.forecast
            )
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _state.value = _state.value.copy(updateChecking = true, updateFailed = false, updateUpToDate = false)
            val currentCode = appVersionCode()
            val result = try {
                updateChecker.check(currentCode)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ru.rufp.weather.update.CheckResult(update = null, networkOk = false)
            }
            if (result.update != null) {
                updateStorage.save(result.update)
                _state.value = _state.value.copy(updateChecking = false, update = result.update)
            } else if (result.networkOk) {
                updateStorage.clear()
                _state.value = _state.value.copy(
                    updateChecking = false,
                    update = null,
                    updateUpToDate = true
                )
            } else {
                _state.value = _state.value.copy(updateChecking = false, updateFailed = true)
            }
            updateStorage.purgeOutdated()
        }
    }

    fun dismissUpdateBanner() {
        viewModelScope.launch { updateStorage.clear() }
    }

    fun downloadUpdate(update: UpdateInfo, onProgress: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                updateDownloader.download(update.apkUrl, update.sha256Url).collect { p ->
                    onProgress(p)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onProgress(-1)
            }
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

    private fun appVersionCode(): Long = updateStorage.installedVersionCode()
}
