package com.joymilk.app.data.model

data class OrderItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",   // snapshot — immutable after creation
    val priceAtOrder: Long = 0L,    // snapshot — immutable after creation
    val quantity: Int = 1,
    val subtotal: Long = 0L,
    val sweetLevel: String = SweetLevel.NORMAL.name
)
