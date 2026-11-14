package ru.rufp.weather.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.rufp.weather.data.repository.ForecastRepository
import ru.rufp.weather.data.repository.SourceRow
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val forecastRepository: ForecastRepository
) : ViewModel() {

    private val _rows = MutableStateFlow<List<SourceRow>>(emptyList())
    val rows: StateFlow<List<SourceRow>> = _rows

    private val _warmUp = MutableStateFlow(false)
    val warmUp: StateFlow<Boolean> = _warmUp

    fun reload() {
        viewModelScope.launch {
            _rows.value = forecastRepository.buildSourceRows()
            _warmUp.value = _rows.value.any { it.warmUp }
        }
    }

    fun toggle(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            forecastRepository.toggleSource(sourceId, enabled)
            reload()
        }
    }
}
