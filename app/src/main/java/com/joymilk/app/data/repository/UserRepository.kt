package com.joymilk.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.joymilk.app.data.model.User
import com.joymilk.app.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    private val usersRef = firestore.collection(Constants.COLLECTION_USERS)

    fun getUsers(): Flow<List<User>> = callbackFlow {
        val listener = usersRef.orderBy("name").addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            trySend(snap?.toObjects(User::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    suspend fun createUser(name: String, email: String, password: String, role: String): Result<Unit> = runCatching {
        val uid = authRepository.createUser(email, password).getOrThrow()
        val user = User(id = uid, name = name, email = email, role = role, isActive = true, createdAt = Timestamp.now())
        usersRef.document(uid).set(user).await()
    }

    suspend fun toggleUserActive(userId: String, isActive: Boolean): Result<Unit> = runCatching {
        usersRef.document(userId).update("isActive", isActive).await()
    }
}

