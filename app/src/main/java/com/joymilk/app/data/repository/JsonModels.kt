package com.joymilk.app.data.repository

import com.joymilk.app.data.model.Order
import com.joymilk.app.data.model.OrderItem
import com.joymilk.app.data.model.PriceHistory
import com.joymilk.app.data.model.Product
import com.joymilk.app.data.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

internal fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { transform(getJSONObject(it)) }

internal fun JSONObject.readOrder() = Order(
    id = getString("id"), customerName = getString("customerName"), customerPhone = getString("customerPhone"),
    deliveryAddress = getString("deliveryAddress"), paymentMethod = getString("paymentMethod"),
    status = getString("status"), totalAmount = getString("totalAmount").toLong(), createdBy = getString("createdBy"),
    createdAt = Instant.parse(getString("createdAt")), updatedAt = Instant.parse(getString("updatedAt")),
    notes = getString("notes")
)
internal fun JSONObject.readItem() = OrderItem(
    id = getString("id"), productId = getString("productId"), productName = getString("productName"),
    priceAtOrder = getString("priceAtOrder").toLong(), quantity = getInt("quantity"),
    subtotal = getString("subtotal").toLong(), sweetLevel = getString("sweetLevel")
)
internal fun JSONObject.readProduct() = Product(
    id = getString("id"), name = getString("name"), category = getString("category"), unit = getString("unit"),
    size = getString("size"), description = getString("description"), currentPrice = getString("currentPrice").toLong(),
    isActive = getBoolean("isActive"), createdAt = Instant.parse(getString("createdAt")),
    updatedAt = Instant.parse(getString("updatedAt"))
)
internal fun JSONObject.readPriceHistory() = PriceHistory(
    id = getString("id"), price = getString("price").toLong(), changedBy = getString("changedBy"),
    effectiveDate = Instant.parse(getString("effectiveDate"))
)
internal fun JSONObject.readUser() = User(
    id = getString("id"), name = getString("name"), email = getString("email"), role = getString("role"),
    isActive = getBoolean("isActive"), createdAt = Instant.parse(getString("createdAt"))
)
