package com.joymilk.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.joymilk.app.util.Constants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun login(email: String, password: String): Result<String> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        getUserRole(auth.currentUser!!.uid)
    }

    suspend fun getUserRole(uid: String): String {
        val doc = firestore.collection(Constants.COLLECTION_USERS).document(uid).get().await()
        return doc.getString("role") ?: "staff"
    }

    suspend fun getUserName(uid: String): String {
        val doc = firestore.collection(Constants.COLLECTION_USERS).document(uid).get().await()
        return doc.getString("name") ?: ""
    }

    suspend fun createUser(email: String, password: String): Result<String> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user!!.uid
    }

    fun logout() = auth.signOut()
}
