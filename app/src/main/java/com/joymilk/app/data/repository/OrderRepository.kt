package com.joymilk.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.joymilk.app.data.model.Order
import com.joymilk.app.data.model.OrderItem
import com.joymilk.app.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val ordersRef = firestore.collection(Constants.COLLECTION_ORDERS)

    suspend fun createOrder(order: Order, items: List<OrderItem>): Result<String> = runCatching {
        // Batch write: order doc + all item sub-docs
        val orderRef = ordersRef.document()
        val batch = firestore.batch()
        batch.set(orderRef, order.copy(createdAt = Timestamp.now(), updatedAt = Timestamp.now()))
        items.forEach { item ->
            val itemRef = orderRef.collection(Constants.SUBCOLLECTION_ITEMS).document()
            batch.set(itemRef, item)
        }
        batch.commit().await()
        orderRef.id
    }

    fun getOrders(statusFilter: String? = null): Flow<List<Order>> = callbackFlow {
        var query: Query = ordersRef.orderBy("createdAt", Query.Direction.DESCENDING)
        if (!statusFilter.isNullOrBlank()) {
            query = query.whereEqualTo("status", statusFilter)
        }
        val listener = query.addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            trySend(snap?.toObjects(Order::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    fun getOrder(orderId: String): Flow<Order?> = callbackFlow {
        val listener = ordersRef.document(orderId).addSnapshotListener { snap, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            trySend(snap?.toObject(Order::class.java))
        }
        awaitClose { listener.remove() }
    }

    fun getOrderItems(orderId: String): Flow<List<OrderItem>> = callbackFlow {
        val listener = ordersRef.document(orderId)
            .collection(Constants.SUBCOLLECTION_ITEMS)
            .addSnapshotListener { snap, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                trySend(snap?.toObjects(OrderItem::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> = runCatching {
        ordersRef.document(orderId).update(
            mapOf("status" to status, "updatedAt" to Timestamp.now())
        ).await()
    }

    suspend fun updateOrder(order: Order, items: List<OrderItem>): Result<Unit> = runCatching {
        val orderRef = ordersRef.document(order.id)
        // Delete existing items then re-add
        val existingItems = orderRef.collection(Constants.SUBCOLLECTION_ITEMS).get().await()
        val batch = firestore.batch()
        existingItems.documents.forEach { batch.delete(it.reference) }
        batch.set(orderRef, order.copy(updatedAt = Timestamp.now()))
        items.forEach { item ->
            val itemRef = orderRef.collection(Constants.SUBCOLLECTION_ITEMS).document()
            batch.set(itemRef, item)
        }
        batch.commit().await()
    }
}
