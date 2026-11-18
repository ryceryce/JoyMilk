package com.joymilk.app.data.repository

import android.net.Uri
import com.joymilk.app.data.model.Order
import com.joymilk.app.data.model.OrderItem
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(private val api: JoyMilkApi) {
    fun getOrders(statusFilter: String? = null) = api.observe("orders") {
        val path = statusFilter?.let { "orders?status=${Uri.encode(it)}" } ?: "orders"
        api.arrayAt(path).mapObjects { it.readOrder() }
    }
    fun observeOrder(id: String) = api.observe("orders") { getOrder(id) }
    suspend fun getOrder(id: String): Pair<Order, List<OrderItem>> {
        val result = api.objectAt("orders/${Uri.encode(id)}")
        return result.readOrder() to result.getJSONArray("items").mapObjects { it.readItem() }
    }

    suspend fun createOrder(order: Order, items: List<OrderItem>, key: String): Result<String> = runCatching {
        val result = api.request("orders", "POST", payload(order, items), key)
        api.invalidate("orders")
        JSONObject(result).getString("id")
    }
    suspend fun updateOrder(order: Order, items: List<OrderItem>): Result<Unit> = runCatching {
        api.request("orders/${Uri.encode(order.id)}", "PATCH", payload(order, items).put("updatedAt", order.updatedAt.toString()))
        api.invalidate("orders")
    }
    suspend fun updateOrderStatus(order: Order, status: String): Result<Unit> = runCatching {
        api.request("orders/${Uri.encode(order.id)}/status", "PATCH",
            JSONObject().put("status", status).put("updatedAt", order.updatedAt.toString()))
        api.invalidate("orders")
    }

    private fun payload(order: Order, items: List<OrderItem>) = JSONObject()
        .put("customerName", order.customerName).put("customerPhone", order.customerPhone)
        .put("deliveryAddress", order.deliveryAddress).put("paymentMethod", order.paymentMethod)
        .put("notes", order.notes)
        .put("items", JSONArray(items.map {
            JSONObject().put("productId", it.productId).put("quantity", it.quantity).put("sweetLevel", it.sweetLevel)
                .apply { if (it.id.isNotBlank()) put("id", it.id) }
        }))
}
