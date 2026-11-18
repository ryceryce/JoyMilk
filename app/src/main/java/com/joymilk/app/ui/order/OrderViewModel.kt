package com.joymilk.app.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joymilk.app.data.model.Order
import com.joymilk.app.data.model.OrderItem
import com.joymilk.app.data.model.Product
import com.joymilk.app.data.model.SweetLevel
import com.joymilk.app.data.repository.ApiFailure
import com.joymilk.app.data.repository.OrderRepository
import com.joymilk.app.data.repository.ProductRepository
import com.joymilk.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

sealed interface OrderListUiState {
    data object Loading : OrderListUiState
    data class Success(val orders: List<Order>) : OrderListUiState
    data class Error(val message: String) : OrderListUiState
}

sealed interface OrderDetailUiState {
    data object Loading : OrderDetailUiState
    data class Success(val order: Order, val items: List<OrderItem>) : OrderDetailUiState
    data class Error(val message: String) : OrderDetailUiState
}

sealed interface OrderFormUiState {
    data object Idle : OrderFormUiState
    data object Loading : OrderFormUiState
    data class Ready(val products: List<Product>, val existing: Pair<Order, List<OrderItem>>?) : OrderFormUiState
    data object Saved : OrderFormUiState
    data class Error(val message: String) : OrderFormUiState
}

data class FormItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val priceAtOrder: Long = 0L,
    val quantity: Int = 1,
    val sweetLevel: String = SweetLevel.NORMAL.name
) {
    val subtotal: Long get() = priceAtOrder * quantity
    fun toOrderItem() = OrderItem(id, productId, productName, priceAtOrder, quantity, subtotal, sweetLevel)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {
    val isOffline = networkMonitor.isOffline.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val _listState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val listState = _listState.asStateFlow()
    private val _detailState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val detailState = _detailState.asStateFlow()
    private val _formState = MutableStateFlow<OrderFormUiState>(OrderFormUiState.Idle)
    val formState = _formState.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError = _saveError.asStateFlow()
    private var orderKey = UUID.randomUUID().toString()
    private var listJob: Job? = null
    private var detailJob: Job? = null

    val statusFilter = MutableStateFlow<String?>(null)

    fun loadOrders() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            combine(statusFilter, isOffline) { filter, offline -> filter to offline }
                .flatMapLatest { (filter, offline) ->
                    if (offline) emptyFlow() else orderRepository.getOrders(filter)
                        .map<List<Order>, OrderListUiState> { OrderListUiState.Success(it) }
                        .catch { emit(OrderListUiState.Error(it.message ?: "Gagal memuat order")) }
                }.collect { _listState.value = it }
        }
    }

    fun loadOrderDetail(orderId: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            isOffline.flatMapLatest { offline ->
                if (offline) emptyFlow() else orderRepository.observeOrder(orderId)
                    .map<Pair<Order, List<OrderItem>>, OrderDetailUiState> { (order, items) ->
                        OrderDetailUiState.Success(order, items)
                    }.catch { emit(OrderDetailUiState.Error(it.message ?: "Gagal memuat detail order")) }
            }.collect { _detailState.value = it }
        }
    }

    fun loadFormProducts(orderId: String?) {
        viewModelScope.launch {
            _formState.value = OrderFormUiState.Loading
            try {
                val existing = orderId?.let { orderRepository.getOrder(it) }
                val products = productRepository.getActiveProducts().first()
                _formState.value = OrderFormUiState.Ready(products, existing)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _formState.value = OrderFormUiState.Error(error.message ?: "Gagal memuat formulir.")
            }
        }
    }

    fun saveOrder(
        customerName: String, customerPhone: String, deliveryAddress: String,
        paymentMethod: String, notes: String, items: List<FormItem>
    ) {
        val ready = _formState.value as? OrderFormUiState.Ready ?: return
        if (_saving.value) return
        if (isOffline.value) {
            _saveError.value = "Koneksi terputus. Order belum dikirim."
            return
        }
        val existing = ready.existing?.first
        val order = (existing ?: Order()).copy(
            customerName = customerName, customerPhone = customerPhone, deliveryAddress = deliveryAddress,
            paymentMethod = paymentMethod, notes = notes
        )
        _saving.value = true
        _saveError.value = null
        viewModelScope.launch {
            val lines = items.map(FormItem::toOrderItem)
            try {
                val result = if (existing == null) orderRepository.createOrder(order, lines, orderKey).map { }
                    else orderRepository.updateOrder(order, lines)
                result.getOrThrow()
                _formState.value = OrderFormUiState.Saved
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _saveError.value = when (error) {
                    is ApiFailure -> if (error.status >= 500) "Status penyimpanan belum diketahui." else error.message
                    is IOException -> "Status penyimpanan belum diketahui."
                    else -> error.message ?: "Gagal menyimpan order."
                }
            } finally {
                _saving.value = false
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        val order = (_detailState.value as? OrderDetailUiState.Success)?.order ?: return
        if (order.id != orderId) return
        viewModelScope.launch {
            orderRepository.updateOrderStatus(order, status)
                .onFailure { _detailState.value = OrderDetailUiState.Error(it.message ?: "Gagal mengubah status.") }
        }
    }
}
