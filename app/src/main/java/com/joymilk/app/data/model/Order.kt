package com.joymilk.app.data.model

import java.time.Instant

data class Order(
    val id: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val paymentMethod: String = "tunai", // "tunai" | "transfer" | "qris"
    val status: String = "pending",       // "pending" | "diproses" | "selesai" | "dibatalkan"
    val totalAmount: Long = 0L,
    val createdBy: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val notes: String = ""
)
