package com.joymilk.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joymilk.app.data.model.Order
import com.joymilk.app.data.repository.AuthRepository
import com.joymilk.app.data.repository.OrderRepository
import com.joymilk.app.data.repository.ReportRepository
import com.joymilk.app.data.repository.ReportData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val uiState: StateFlow<DashboardUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val uid = authRepository.currentUser?.uid ?: ""

            // Get user display name via AuthRepository
            val nameResult = runCatching { authRepository.getUserName(uid) }

            reportRepository.getTodaySummary()
                .onSuccess { summary ->
                    _uiState.update { it.copy(todaySummary = summary, userName = nameResult.getOrDefault("")) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }

            // Collect 5 most recent orders
            orderRepository.getOrders()
                .take(1)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { orders ->
                    _uiState.update { it.copy(isLoading = false, recentOrders = orders.take(5)) }
                }
        }
    }
}


