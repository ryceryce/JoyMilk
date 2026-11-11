package com.joymilk.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joymilk.app.data.model.PriceHistory
import com.joymilk.app.data.model.Product
import com.joymilk.app.data.repository.AuthRepository
import com.joymilk.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Success(val products: List<Product>) : ProductListUiState
    data class Error(val message: String) : ProductListUiState
}

sealed interface ProductFormUiState {
    data object Idle : ProductFormUiState
    data object Loading : ProductFormUiState
    data class Loaded(val product: Product) : ProductFormUiState
    data object Saved : ProductFormUiState
    // product included so form can re-display with original data after error
    data class Error(val message: String, val product: Product = Product()) : ProductFormUiState
}

sealed interface PriceHistoryUiState {
    data object Loading : PriceHistoryUiState
    data class Success(val history: List<PriceHistory>) : PriceHistoryUiState
    data class Error(val message: String) : PriceHistoryUiState
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val listState: StateFlow<ProductListUiState> = _listState

    private val _formState = MutableStateFlow<ProductFormUiState>(ProductFormUiState.Idle)
    val formState: StateFlow<ProductFormUiState> = _formState

    private val _priceHistoryState = MutableStateFlow<PriceHistoryUiState>(PriceHistoryUiState.Loading)
    val priceHistoryState: StateFlow<PriceHistoryUiState> = _priceHistoryState

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    var categoryFilter = MutableStateFlow("")
    var searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            productRepository.getCategories()
                .catch { }
                .collect { _categories.value = it }
        }
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            productRepository.getAllProducts()
                .catch { e -> _listState.value = ProductListUiState.Error(e.message ?: "Gagal memuat produk") }
                .collect { _listState.value = ProductListUiState.Success(it) }
        }
    }

    fun loadProduct(productId: String?) {
        if (productId == null) {
            _formState.value = ProductFormUiState.Loaded(Product())
            return
        }
        // For edit: find from existing list or load fresh
        val existing = (_listState.value as? ProductListUiState.Success)
            ?.products?.find { it.id == productId }
        if (existing != null) {
            _formState.value = ProductFormUiState.Loaded(existing)
        } else {
            _formState.value = ProductFormUiState.Loading
            // Will be populated when list loads
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            _formState.value = ProductFormUiState.Loading
            val uid = authRepository.currentUser?.uid ?: ""
            val result = if (product.id.isEmpty()) {
                productRepository.addProduct(product)
                    .map { }
            } else {
                productRepository.updateProduct(product, uid)
            }
            result
                .onSuccess { _formState.value = ProductFormUiState.Saved }
                .onFailure { e -> _formState.value = ProductFormUiState.Error(e.message ?: "Gagal menyimpan produk", product) }
        }
    }

    fun loadPriceHistory(productId: String) {
        viewModelScope.launch {
            productRepository.getPriceHistory(productId)
                .catch { e -> _priceHistoryState.value = PriceHistoryUiState.Error(e.message ?: "Gagal memuat riwayat harga") }
                .collect { _priceHistoryState.value = PriceHistoryUiState.Success(it) }
        }
    }

    fun resetFormState() {
        _formState.value = ProductFormUiState.Idle
    }

    fun addCategory(name: String) {
        viewModelScope.launch { productRepository.addCategory(name) }
    }
}
