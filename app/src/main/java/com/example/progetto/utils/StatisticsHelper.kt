package com.example.progetto.utils

import com.example.progetto.data.entity.Trip
import com.example.progetto.data.entity.TripType
import java.text.SimpleDateFormat
import java.util.*

object StatisticsHelper {
    fun getMonthlyTripCount(trips: List<Trip>): Map<String, Int> {
        return trips
            .groupBy { getYearMonth(it.startDate) }
            .mapValues { it.value.size }
            .toSortedMap()  // 按月份排序
    }

    fun getMonthlyDistance(trips: List<Trip>): Map<String, Double> {
        return trips
            .groupBy { getYearMonth(it.startDate) }
            .mapValues { entry ->
                entry.value.sumOf { it.distance }
            }
            .toSortedMap()
    }

    fun getTripTypeDistribution(trips: List<Trip>): Map<TripType, Int> {
        return trips
            .groupBy { it.type }
            .mapValues { it.value.size }
    }

    fun getRecentMonths(months: Int): List<String> {
        val result = mutableListOf<String>()
        val calendar = Calendar.getInstance()

        for (i in 0 until months) {
            result.add(getYearMonth(calendar.time))
            calendar.add(Calendar.MONTH, -1)
        }

        return result.reversed()  // 从早到晚排序
    }

    fun formatMonth(yearMonth: String): String {
        return try {
            val parts = yearMonth.split("-")
            "${parts[1].toInt()}月"
        } catch (e: Exception) {
            yearMonth
        }
    }

    private fun getYearMonth(dateString: String): String {
        return dateString.substring(0, 7)
    }

    /**
     * 获取年月字符串（从Date）
     */
    private fun getYearMonth(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * 计算总统计数据
     */
    data class TotalStats(
        val totalTrips: Int,
        val totalDistance: Double,
        val averageDistance: Double,
        val longestTrip: Trip?,
        val mostVisitedMonth: String?
    )

    fun getTotalStats(trips: List<Trip>): TotalStats {
        if (trips.isEmpty()) {
            return TotalStats(0, 0.0, 0.0, null, null)
        }

        val totalDistance = trips.sumOf { it.distance }
        val averageDistance = totalDistance / trips.size
        val longestTrip = trips.maxByOrNull { it.distance }

        val mostVisitedMonth = trips
            .groupBy { getYearMonth(it.startDate) }
            .maxByOrNull { it.value.size }
            ?.key

        return TotalStats(
            totalTrips = trips.size,
            totalDistance = totalDistance,
            averageDistance = averageDistance,
            longestTrip = longestTrip,
            mostVisitedMonth = mostVisitedMonth
        )
    }
}