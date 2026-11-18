package com.joymilk.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joymilk.app.data.repository.ReportData
import com.joymilk.app.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class ReportPeriod { HARIAN, MINGGUAN, BULANAN }

data class ReportUiState(
    val isLoading: Boolean = true,
    val period: ReportPeriod = ReportPeriod.HARIAN,
    val data: ReportData = ReportData(),
    val error: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(private val reportRepository: ReportRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { loadReport(ReportPeriod.HARIAN) }

    fun loadReport(period: ReportPeriod) {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, period = period, error = null) }
        loadJob = viewModelScope.launch {
            reportRepository.changes().onStart { emit("*") }.collect {
                val today = LocalDate.now(ZoneId.of("Asia/Jakarta"))
                val start = today.minusDays(when (period) {
                    ReportPeriod.HARIAN -> 0
                    ReportPeriod.MINGGUAN -> 6
                    ReportPeriod.BULANAN -> 29
                })
                reportRepository.getReport(start, today)
                    .onSuccess { data -> _uiState.update { it.copy(isLoading = false, data = data, error = null) } }
                    .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
            }
        }
    }
}
