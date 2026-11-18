package com.joymilk.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joymilk.app.data.model.Order
import com.joymilk.app.data.repository.AuthRepository
import com.joymilk.app.data.repository.OrderRepository
import com.joymilk.app.data.repository.ReportData
import com.joymilk.app.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val todaySummary: ReportData = ReportData(),
    val recentOrders: List<Order> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val name = runCatching { authRepository.getUserName() }.getOrDefault("")
            orderRepository.getOrders()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { orders ->
                    val summary = reportRepository.getTodaySummary()
                    _uiState.update {
                        it.copy(isLoading = false, userName = name, recentOrders = orders.take(5),
                            todaySummary = summary.getOrDefault(it.todaySummary), error = summary.exceptionOrNull()?.message)
                    }
                }
        }
    }
}
