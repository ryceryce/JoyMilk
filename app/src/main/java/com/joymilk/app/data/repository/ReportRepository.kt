package com.joymilk.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.joymilk.app.data.model.Order
import com.joymilk.app.data.model.OrderItem
import com.joymilk.app.util.Constants
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class ReportData(
    val totalRevenue: Long = 0L,
    val orderCount: Int = 0,
    val topProducts: List<Pair<String, Int>> = emptyList() // productName to quantity
)

@Singleton
class ReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val ordersRef = firestore.collection(Constants.COLLECTION_ORDERS)

    suspend fun getReport(startDate: Timestamp, endDate: Timestamp): Result<ReportData> = runCatching {
        val snap = ordersRef
            .whereGreaterThanOrEqualTo("createdAt", startDate)
            .whereLessThanOrEqualTo("createdAt", endDate)
            .whereNotEqualTo("status", Constants.STATUS_DIBATALKAN)
            .get().await()

        val orders = snap.toObjects(Order::class.java)
        val totalRevenue = orders.sumOf { it.totalAmount }
        val orderCount = orders.size

        // Aggregate top products from items subcollection
        val productQty = mutableMapOf<String, Int>()
        orders.forEach { order ->
            val itemsSnap = ordersRef.document(order.id)
                .collection(Constants.SUBCOLLECTION_ITEMS).get().await()
            itemsSnap.toObjects(OrderItem::class.java).forEach { item ->
                productQty[item.productName] = (productQty[item.productName] ?: 0) + item.quantity
            }
        }
        val topProducts = productQty.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        ReportData(totalRevenue, orderCount, topProducts)
    }

    // Convenience: today's summary for dashboard
    suspend fun getTodaySummary(): Result<ReportData> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val start = Timestamp(cal.time)
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val end = Timestamp(cal.time)
        return getReport(start, end)
    }
}
