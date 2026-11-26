package ru.rufp.weather.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.rufp.weather.data.local.SourceMetricsDao
import ru.rufp.weather.data.repository.ForecastRepository
import ru.rufp.weather.data.repository.SourceRow
import ru.rufp.weather.update.CheckResult
import ru.rufp.weather.update.UpdateChecker
import ru.rufp.weather.update.UpdateStorage
import javax.inject.Inject

data class AboutUiState(
    val rows: List<SourceRow> = emptyList(),
    val warmUp: Boolean = false,
    val observationCount: Int = 0,
    val checkingUpdates: Boolean = false,
    val updateUpToDate: Boolean = false,
    val updateFailed: Boolean = false,
    val latestTag: String? = null
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val forecastRepository: ForecastRepository,
    private val sourceMetricsDao: SourceMetricsDao,
    private val updateChecker: UpdateChecker,
    private val updateStorage: UpdateStorage
) : ViewModel() {

    private val _state = MutableStateFlow(AboutUiState())
    val state: StateFlow<AboutUiState> = _state

    fun load() {
        viewModelScope.launch {
            val rows = forecastRepository.buildSourceRows()
            _state.value = _state.value.copy(
                rows = rows,
                warmUp = rows.any { it.warmUp },
                observationCount = sourceMetricsDao.observationCount()
            )
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _state.value = _state.value.copy(checkingUpdates = true, updateFailed = false, updateUpToDate = false)
            val currentCode = updateStorage.installedVersionCode()
            val result = try {
                updateChecker.check(currentCode)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                CheckResult(update = null, networkOk = false)
            }
            if (result.update != null) {
                updateStorage.save(result.update)
                _state.value = _state.value.copy(
                    checkingUpdates = false,
                    latestTag = result.update.versionName
                )
            } else if (result.networkOk) {
                updateStorage.clear()
                _state.value = _state.value.copy(
                    checkingUpdates = false,
                    updateUpToDate = true,
                    latestTag = null
                )
            } else {
                _state.value = _state.value.copy(checkingUpdates = false, updateFailed = true)
            }
            updateStorage.purgeOutdated()
        }
    }
}
