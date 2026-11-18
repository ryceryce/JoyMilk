package com.joymilk.app.data.model

import java.time.Instant

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val size: String = "",
    val description: String = "",
    val currentPrice: Long = 0L,
    val unit: String = "",
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
