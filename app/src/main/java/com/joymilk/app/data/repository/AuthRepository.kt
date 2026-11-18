package com.joymilk.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val api: JoyMilkApi
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun login(email: String, password: String): Result<String> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        runCatching { getUserRole() }.onFailure { auth.signOut() }.getOrThrow()
    }

    suspend fun getUserRole(): String = api.objectAt("me").getString("role")
    suspend fun getUserName(): String = api.objectAt("me").getString("name")

    fun logout() = auth.signOut()
}
