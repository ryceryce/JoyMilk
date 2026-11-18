package com.joymilk.app.data.model

import java.time.Instant

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "staff", // "admin" | "staff"
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now()
)
