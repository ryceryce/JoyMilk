package com.joymilk.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.joymilk.app.data.repository.ReportData
import com.joymilk.app.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class ReportPeriod { HARIAN, MINGGUAN, BULANAN }

data class ReportUiState(
    val isLoading: Boolean = true,
    val period: ReportPeriod = ReportPeriod.HARIAN,
    val data: ReportData = ReportData(),
    val error: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState

    init { loadReport(ReportPeriod.HARIAN) }

    fun loadReport(period: ReportPeriod) {
        _uiState.update { it.copy(isLoading = true, period = period) }
        val (start, end) = dateRangeFor(period)
        viewModelScope.launch {
            reportRepository.getReport(start, end)
                .onSuccess { data -> _uiState.update { it.copy(isLoading = false, data = data) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun dateRangeFor(period: ReportPeriod): Pair<Timestamp, Timestamp> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val end = Timestamp(cal.time)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        when (period) {
            ReportPeriod.HARIAN -> { /* today already set */ }
            ReportPeriod.MINGGUAN -> cal.add(Calendar.DAY_OF_YEAR, -6)
            ReportPeriod.BULANAN -> cal.add(Calendar.DAY_OF_YEAR, -29)
        }
        return Timestamp(cal.time) to end
    }
}
