package com.joymilk.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "",
    val size: String = "",
    val description: String = "",
    val currentPrice: Long = 0L,
    val unit: String = "",
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
