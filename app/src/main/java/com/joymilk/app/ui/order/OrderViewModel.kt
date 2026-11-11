package com.joymilk.app.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joymilk.app.data.model.Order
import com.joymilk.app.data.model.OrderItem
import com.joymilk.app.data.model.Product
import com.joymilk.app.data.model.SweetLevel
import com.joymilk.app.data.repository.AuthRepository
import com.joymilk.app.data.repository.OrderRepository
import com.joymilk.app.data.repository.ProductRepository
import com.joymilk.app.util.Constants
import com.joymilk.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    data class Ready(val products: List<Product>) : OrderFormUiState
    data object Saved : OrderFormUiState
    data class Error(val message: String) : OrderFormUiState
}

// Mutable form item state
data class FormItem(
    val productId: String = "",
    val productName: String = "",
    val priceAtOrder: Long = 0L,
    val quantity: Int = 1,
    val sweetLevel: String = SweetLevel.NORMAL.name
) {
    val subtotal: Long get() = priceAtOrder * quantity
    fun toOrderItem() = OrderItem(
        productId = productId,
        productName = productName,
        priceAtOrder = priceAtOrder,
        quantity = quantity,
        subtotal = subtotal,
        sweetLevel = sweetLevel
    )
}

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val isOffline: StateFlow<Boolean> = networkMonitor.isOffline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _listState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val listState: StateFlow<OrderListUiState> = _listState

    private val _detailState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val detailState: StateFlow<OrderDetailUiState> = _detailState

    private val _formState = MutableStateFlow<OrderFormUiState>(OrderFormUiState.Idle)
    val formState: StateFlow<OrderFormUiState> = _formState

    var statusFilter = MutableStateFlow<String?>(null)

    fun loadOrders() {
        viewModelScope.launch {
            statusFilter.flatMapLatest { filter ->
                orderRepository.getOrders(filter)
            }
                .catch { e -> _listState.value = OrderListUiState.Error(e.message ?: "Gagal memuat order") }
                .collect { _listState.value = OrderListUiState.Success(it) }
        }
    }

    fun loadOrderDetail(orderId: String) {
        viewModelScope.launch {
            combine(
                orderRepository.getOrder(orderId),
                orderRepository.getOrderItems(orderId)
            ) { order, items -> order to items }
                .catch { e -> _detailState.value = OrderDetailUiState.Error(e.message ?: "Gagal memuat detail order") }
                .collect { (order, items) ->
                    if (order != null) _detailState.value = OrderDetailUiState.Success(order, items)
                    else _detailState.value = OrderDetailUiState.Error("Order tidak ditemukan")
                }
        }
    }

    fun loadFormProducts() {
        viewModelScope.launch {
            _formState.value = OrderFormUiState.Loading
            productRepository.getActiveProducts()
                .catch { e -> _formState.value = OrderFormUiState.Error(e.message ?: "Gagal memuat produk") }
                .collect { products -> _formState.value = OrderFormUiState.Ready(products) }
        }
    }

    fun saveOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        paymentMethod: String,
        notes: String,
        items: List<FormItem>
    ) {
        val uid = authRepository.currentUser?.uid ?: ""
        val totalAmount = items.sumOf { it.subtotal }
        val order = Order(
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            paymentMethod = paymentMethod,
            notes = notes,
            totalAmount = totalAmount,
            createdBy = uid
        )
        viewModelScope.launch {
            val prevState = _formState.value
            _formState.value = OrderFormUiState.Loading
            orderRepository.createOrder(order, items.map { it.toOrderItem() })
                .onSuccess { _formState.value = OrderFormUiState.Saved }
                .onFailure { e ->
                    _formState.value = if (prevState is OrderFormUiState.Ready)
                        OrderFormUiState.Error(e.message ?: "Gagal menyimpan order")
                    else OrderFormUiState.Error(e.message ?: "Gagal menyimpan order")
                }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, status)
        }
    }

    fun resetFormState() { _formState.value = OrderFormUiState.Idle }
}
