package com.joymilk.app.data.repository

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class ReportData(
    val totalRevenue: Long = 0,
    val orderCount: Int = 0,
    val topProducts: List<Pair<String, Long>> = emptyList()
)

@Singleton
class ReportRepository @Inject constructor(private val api: JoyMilkApi) {
    fun changes() = api.changes("orders")

    suspend fun getReport(startDate: LocalDate, endDate: LocalDate): Result<ReportData> = runCatching {
        val report = api.objectAt("reports?from=$startDate&to=$endDate")
        ReportData(report.getString("totalRevenue").toLong(), report.getInt("orderCount"),
            report.getJSONArray("topProducts").mapObjects { it.getString("name") to it.getString("quantity").toLong() })
    }

    suspend fun getTodaySummary(): Result<ReportData> {
        val today = LocalDate.now(ZoneId.of("Asia/Jakarta"))
        return getReport(today, today)
    }
}
