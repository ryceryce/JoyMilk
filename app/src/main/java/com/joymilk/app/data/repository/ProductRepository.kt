package com.joymilk.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.joymilk.app.data.model.PriceHistory
import com.joymilk.app.data.model.Product
import com.joymilk.app.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val productsRef = firestore.collection(Constants.COLLECTION_PRODUCTS)
    private val categoriesDoc = firestore
        .collection(Constants.COLLECTION_SETTINGS)
        .document(Constants.SETTINGS_DOC_CATEGORIES)

    private val defaultCategories = listOf("Susu Kedelai", "Tahu", "Tempe", "Lainnya")

    fun getActiveProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsRef
            .whereEqualTo("isActive", true)
            .orderBy("name")
            .addSnapshotListener { snap, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(snap?.toObjects(Product::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsRef
            .orderBy("name")
            .addSnapshotListener { snap, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(snap?.toObjects(Product::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addProduct(product: Product): Result<String> = runCatching {
        val ref = productsRef.add(product).await()
        ref.id
    }

    suspend fun updateProduct(product: Product, changedBy: String): Result<Unit> = runCatching {
        val existingSnap = productsRef.document(product.id).get().await()
        val existing = existingSnap.toObject(Product::class.java)
        val priceChanged = existing != null && existing.currentPrice != product.currentPrice

        if (priceChanged) {
            // Firestore transaction: update price + add price history atomically
            firestore.runTransaction { tx ->
                val productRef = productsRef.document(product.id)
                val historyRef = productRef.collection(Constants.SUBCOLLECTION_PRICE_HISTORY).document()
                tx.set(productRef, product.copy(updatedAt = Timestamp.now()))
                tx.set(
                    historyRef,
                    PriceHistory(
                        price = product.currentPrice,
                        effectiveDate = Timestamp.now(),
                        changedBy = changedBy
                    )
                )
            }.await()
        } else {
            productsRef.document(product.id)
                .set(product.copy(updatedAt = Timestamp.now()))
                .await()
        }
    }

    suspend fun updateProductActiveStatus(id: String, isActive: Boolean): Result<Unit> = runCatching {
        productsRef.document(id).update("isActive", isActive).await()
    }

    fun getPriceHistory(productId: String): Flow<List<PriceHistory>> = callbackFlow {
        val listener = productsRef.document(productId)
            .collection(Constants.SUBCOLLECTION_PRICE_HISTORY)
            .orderBy("effectiveDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(snap?.toObjects(PriceHistory::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getCategories(): Flow<List<String>> = callbackFlow {
        val listener = categoriesDoc.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            @Suppress("UNCHECKED_CAST")
            val list = snap?.get("list") as? List<String>
            trySend(list ?: defaultCategories)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addCategory(name: String): Result<Unit> = runCatching {
        val snap = categoriesDoc.get().await()
        @Suppress("UNCHECKED_CAST")
        val current = (snap.get("list") as? List<String>) ?: defaultCategories
        if (!current.contains(name)) {
            categoriesDoc.set(mapOf("list" to current + name)).await()
        }
    }
}

