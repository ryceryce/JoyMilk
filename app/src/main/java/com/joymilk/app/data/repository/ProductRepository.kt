package com.joymilk.app.data.repository

import android.net.Uri
import com.joymilk.app.data.model.Product
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(private val api: JoyMilkApi) {
    fun getActiveProducts() = api.observe("products") { products("products?active=true") }
    fun getAllProducts() = api.observe("products", "categories") { products("products") }

    suspend fun getProduct(id: String): Product = products("products").firstOrNull { it.id == id }
        ?: error("Produk tidak ditemukan.")

    private suspend fun products(path: String) = api.arrayAt(path).mapObjects { it.readProduct() }

    suspend fun addProduct(product: Product): Result<String> = runCatching {
        val result = api.request("products", "POST", product.payload())
        api.invalidate("products")
        JSONObject(result).getString("id")
    }
    suspend fun updateProduct(product: Product): Result<Unit> = runCatching {
        api.request("products/${Uri.encode(product.id)}", "PATCH", product.payload())
        api.invalidate("products")
    }
    suspend fun updateProductActiveStatus(id: String, isActive: Boolean): Result<Unit> = runCatching {
        api.request("products/${Uri.encode(id)}", "PATCH", JSONObject().put("isActive", isActive))
        api.invalidate("products")
    }
    fun getPriceHistory(id: String) = api.observe("products") {
        api.arrayAt("products/${Uri.encode(id)}/price-history").mapObjects { it.readPriceHistory() }
    }
    fun getCategories() = api.observe("categories") {
        api.arrayAt("categories").mapObjects { it.getString("name") }
    }
    suspend fun addCategory(name: String): Result<Unit> = runCatching {
        api.request("categories", "POST", JSONObject().put("name", name))
        api.invalidate("categories")
    }

    private fun Product.payload() = JSONObject().put("name", name).put("category", category)
        .put("size", size).put("description", description).put("unit", unit)
        .put("currentPrice", currentPrice.toString()).put("isActive", isActive)
}
