package com.joymilk.app.data.repository

import android.net.Uri
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val api: JoyMilkApi) {
    fun getUsers() = api.observe("users") { api.arrayAt("users").mapObjects { it.readUser() } }

    suspend fun createUser(name: String, email: String, password: String, role: String): Result<Unit> = runCatching {
        api.request("users", "POST", JSONObject().put("name", name).put("email", email)
            .put("password", password).put("role", role))
        api.invalidate("users")
    }
    suspend fun toggleUserActive(userId: String, isActive: Boolean): Result<Unit> = runCatching {
        api.request("users/${Uri.encode(userId)}", "PATCH", JSONObject().put("isActive", isActive))
        api.invalidate("users")
    }
}
