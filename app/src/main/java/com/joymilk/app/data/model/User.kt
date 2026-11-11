package com.joymilk.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "staff", // "admin" | "staff"
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now()
)
