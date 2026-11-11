package com.joymilk.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Order(
    @DocumentId val id: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val paymentMethod: String = "tunai", // "tunai" | "transfer" | "qris"
    val status: String = "pending",       // "pending" | "diproses" | "selesai" | "dibatalkan"
    val totalAmount: Long = 0L,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val notes: String = ""
)
