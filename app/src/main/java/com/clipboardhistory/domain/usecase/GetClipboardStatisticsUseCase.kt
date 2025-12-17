package com.clipboardhistory.domain.usecase

import com.clipboardhistory.domain.repository.ClipboardRepository
import com.clipboardhistory.domain.repository.ClipboardStatistics
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for retrieving clipboard usage statistics.
 *
 * This use case provides comprehensive statistics about clipboard usage
 * including usage patterns, content types, and activity metrics.
 */
class GetClipboardStatisticsUseCase @Inject constructor(
    private val repository: ClipboardRepository,
) {
    /**
     * Get comprehensive clipboard statistics.
     *
     * @return ClipboardStatistics containing usage metrics
     */
    suspend operator fun invoke(): ClipboardStatistics {
        return repository.getStatistics()
    }

    /**
     * Get statistics for a specific time period.
     *
     * @param startTime Start of the time period
     * @param endTime End of the time period
     * @return Statistics for the specified time period
     */
    suspend fun getStatisticsForPeriod(
        startTime: Long,
        endTime: Long,
    ): ClipboardStatistics {
        val itemsInPeriod = repository.getItemsByTimestampRange(startTime, endTime)

        val favoriteItems = itemsInPeriod.count { it.isFavorite }
        val mostUsedContentType = itemsInPeriod.groupBy { it.contentType }
            .mapValues { it.value.size }
            .maxByOrNull { it.value }?.key?.name ?: "TEXT"

        val averageContentLength = if (itemsInPeriod.isNotEmpty()) {
            itemsInPeriod.sumOf { it.content.length } / itemsInPeriod.size
        } else 0

        val lastActivityTimestamp = itemsInPeriod.maxOfOrNull { it.timestamp } ?: 0L

        return ClipboardStatistics(
            totalItems = itemsInPeriod.size,
            favoriteItems = favoriteItems,
            itemsToday = itemsInPeriod.count {
                val itemTime = it.timestamp
                val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                itemTime >= todayStart
            },
            itemsThisWeek = itemsInPeriod.count {
                val itemTime = it.timestamp
                val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                itemTime >= weekStart
            },
            mostUsedContentType = mostUsedContentType,
            averageContentLength = averageContentLength,
            lastActivityTimestamp = lastActivityTimestamp,
        )
    }

    /**
     * Get content type distribution.
     *
     * @return Map of content types to their usage counts
     */
    suspend fun getContentTypeDistribution(): Map<String, Int> {
        val allItems = repository.getAllItems().first()
        return allItems.groupBy { it.contentType.name }
            .mapValues { it.value.size }
            .toSortedMap(compareByDescending { it }) // Sort by count descending
    }

    /**
     * Get hourly activity pattern.
     *
     * @return Map of hour (0-23) to activity count
     */
    suspend fun getHourlyActivityPattern(): Map<Int, Int> {
        val allItems = repository.getAllItems().first()
        val hourlyPattern = mutableMapOf<Int, Int>()

        // Initialize all hours with 0
        for (hour in 0..23) {
            hourlyPattern[hour] = 0
        }

        allItems.forEach { item ->
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = item.timestamp
            }
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            hourlyPattern[hour] = hourlyPattern.getOrDefault(hour, 0) + 1
        }

        return hourlyPattern.toSortedMap()
    }

    /**
     * Get daily activity for the last N days.
     *
     * @param days Number of days to look back
     * @return Map of relative days (0 = today, -1 = yesterday) to activity count
     */
    suspend fun getDailyActivity(days: Int = 7): Map<Int, Int> {
        val allItems = repository.getAllItems().first()
        val currentTime = System.currentTimeMillis()
        val dailyActivity = mutableMapOf<Int, Int>()

        // Initialize days with 0
        for (dayOffset in 0 downTo -(days - 1)) {
            dailyActivity[dayOffset] = 0
        }

        allItems.forEach { item ->
            val daysDiff = ((currentTime - item.timestamp) / (1000 * 60 * 60 * 24)).toInt()
            if (daysDiff in 0 until days) {
                dailyActivity[-daysDiff] = dailyActivity.getOrDefault(-daysDiff, 0) + 1
            }
        }

        return dailyActivity.toSortedMap()
    }

    /**
     * Get clipboard efficiency metrics.
     *
     * @return Map containing efficiency metrics
     */
    suspend fun getEfficiencyMetrics(): Map<String, Any> {
        val allItems = repository.getAllItems().first()
        val currentTime = System.currentTimeMillis()
        val oneDayAgo = currentTime - (24 * 60 * 60 * 1000L)

        val itemsToday = allItems.count { it.timestamp >= oneDayAgo }
        val duplicateItems = allItems.groupBy { it.content }
            .filter { it.value.size > 1 }
            .flatMap { it.value.drop(1) } // Keep only duplicates

        val averageItemsPerDay = if (allItems.isNotEmpty()) {
            val oldestItem = allItems.minByOrNull { it.timestamp }?.timestamp ?: currentTime
            val daysSinceFirstItem = ((currentTime - oldestItem) / (1000 * 60 * 60 * 24.0)).coerceAtLeast(1.0)
            allItems.size / daysSinceFirstItem
        } else 0.0

        return mapOf(
            "itemsToday" to itemsToday,
            "duplicateCount" to duplicateItems.size,
            "averageItemsPerDay" to averageItemsPerDay,
            "uniqueContentRatio" to if (allItems.isNotEmpty()) {
                (allItems.size - duplicateItems.size).toDouble() / allItems.size
            } else 1.0,
            "mostProductiveHour" to (getHourlyActivityPattern().maxByOrNull { it.value }?.key ?: -1),
            "clipboardRetentionRate" to calculateRetentionRate(),
        )
    }

    /**
     * Calculate clipboard retention rate (percentage of items still useful after time).
     *
     * @return Retention rate as a percentage (0.0 to 1.0)
     */
    private suspend fun calculateRetentionRate(): Double {
        val allItems = repository.getAllItems().first()
        val currentTime = System.currentTimeMillis()
        val oneWeekAgo = currentTime - (7 * 24 * 60 * 60 * 1000L)

        val recentItems = allItems.filter { it.timestamp >= oneWeekAgo }
        val retainedItems = recentItems.count { !it.isDeleted }

        return if (recentItems.isNotEmpty()) {
            retainedItems.toDouble() / recentItems.size
        } else 1.0
    }

    /**
     * Get application usage statistics (which apps create clipboard content).
     *
     * @return Map of application names to usage counts
     */
    suspend fun getApplicationUsageStats(): Map<String, Int> {
        val allItems = repository.getAllItems().first()

        return allItems.groupBy { it.sourceApp ?: "Unknown" }
            .mapValues { it.value.size }
            .toSortedMap(compareByDescending { it })
    }

    /**
     * Get clipboard health metrics.
     *
     * @return Map containing health-related metrics
     */
    suspend fun getHealthMetrics(): Map<String, Any> {
        val stats = invoke()
        val allItems = repository.getAllItems().first()

        val averageTimeBetweenCopies = if (allItems.size > 1) {
            val sortedTimestamps = allItems.map { it.timestamp }.sorted()
            val intervals = sortedTimestamps.zipWithNext { a, b -> b - a }
            intervals.average()
        } else 0.0

        return mapOf(
            "databaseHealth" to "good", // Could be enhanced with actual DB checks
            "averageTimeBetweenCopies" to averageTimeBetweenCopies,
            "contentTypeDiversity" to stats.mostUsedContentType.let {
                val typeCount = allItems.groupBy { it.contentType }.size
                if (typeCount > 5) "high" else if (typeCount > 2) "medium" else "low"
            },
            "usageConsistency" to calculateUsageConsistency(),
            "lastBackupAge" to 0L, // Would need backup tracking implementation
        )
    }

    /**
     * Calculate usage consistency score (how regular clipboard usage is).
     *
     * @return Consistency score (0.0 to 1.0, higher is more consistent)
     */
    private suspend fun calculateUsageConsistency(): Double {
        val dailyActivity = getDailyActivity(30)
        if (dailyActivity.isEmpty()) return 0.0

        val values = dailyActivity.values
        val mean = values.average()
        val variance = if (mean > 0) {
            values.map { (it - mean) * (it - mean) }.average()
        } else 0.0

        val stdDev = kotlin.math.sqrt(variance)
        val coefficientOfVariation = if (mean > 0) stdDev / mean else 0.0

        // Lower coefficient of variation means more consistent usage
        return (1.0 - coefficientOfVariation.coerceIn(0.0, 1.0))
    }
}