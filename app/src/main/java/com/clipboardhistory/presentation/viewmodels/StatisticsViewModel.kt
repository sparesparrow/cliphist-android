package com.clipboardhistory.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardhistory.domain.usecase.GetClipboardStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for statistics screen functionality.
 *
 * Provides clipboard usage statistics and analytics data.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getClipboardStatisticsUseCase: GetClipboardStatisticsUseCase,
) : ViewModel() {

    private val _statistics = MutableStateFlow<com.clipboardhistory.domain.repository.ClipboardStatistics?>(null)
    val statistics: StateFlow<com.clipboardhistory.domain.repository.ClipboardStatistics?> = _statistics.asStateFlow()

    private val _efficiencyMetrics = MutableStateFlow<Map<String, Any>>(emptyMap())
    val efficiencyMetrics: StateFlow<Map<String, Any>> = _efficiencyMetrics.asStateFlow()

    private val _contentTypeDistribution = MutableStateFlow<Map<String, Int>>(emptyMap())
    val contentTypeDistribution: StateFlow<Map<String, Int>> = _contentTypeDistribution.asStateFlow()

    private val _hourlyActivity = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val hourlyActivity: StateFlow<Map<Int, Int>> = _hourlyActivity.asStateFlow()

    init {
        loadStatistics()
    }

    /**
     * Load all statistics data.
     */
    fun loadStatistics() {
        viewModelScope.launch {
            try {
                // Load basic statistics
                _statistics.value = getClipboardStatisticsUseCase()

                // Load efficiency metrics
                val efficiency = getClipboardStatisticsUseCase.getEfficiencyMetrics()
                _efficiencyMetrics.value = efficiency

                // Load content type distribution
                val contentTypes = getClipboardStatisticsUseCase.getContentTypeDistribution()
                _contentTypeDistribution.value = contentTypes

                // Load hourly activity
                val hourly = getClipboardStatisticsUseCase.getHourlyActivityPattern()
                _hourlyActivity.value = hourly

            } catch (e: Exception) {
                // Handle errors gracefully
                _statistics.value = null
                _efficiencyMetrics.value = emptyMap()
                _contentTypeDistribution.value = emptyMap()
                _hourlyActivity.value = emptyMap()
            }
        }
    }

    /**
     * Refresh all statistics data.
     */
    fun refreshStatistics() {
        loadStatistics()
    }

    /**
     * Get statistics for a specific time period.
     */
    fun getStatisticsForPeriod(
        startTime: Long,
        endTime: Long,
    ): StateFlow<com.clipboardhistory.domain.repository.ClipboardStatistics?> {
        val periodStats = MutableStateFlow<com.clipboardhistory.domain.repository.ClipboardStatistics?>(null)

        viewModelScope.launch {
            try {
                val stats = getClipboardStatisticsUseCase.getStatisticsForPeriod(startTime, endTime)
                periodStats.value = stats
            } catch (e: Exception) {
                periodStats.value = null
            }
        }

        return periodStats.asStateFlow()
    }

    /**
     * Get application usage statistics.
     */
    fun getApplicationUsageStats(): StateFlow<Map<String, Int>> {
        val appStats = MutableStateFlow<Map<String, Int>>(emptyMap())

        viewModelScope.launch {
            try {
                val stats = getClipboardStatisticsUseCase.getApplicationUsageStats()
                appStats.value = stats
            } catch (e: Exception) {
                appStats.value = emptyMap()
            }
        }

        return appStats.asStateFlow()
    }

    /**
     * Get health metrics for the clipboard system.
     */
    fun getHealthMetrics(): StateFlow<Map<String, Any>> {
        val healthMetrics = MutableStateFlow<Map<String, Any>>(emptyMap())

        viewModelScope.launch {
            try {
                val metrics = getClipboardStatisticsUseCase.getHealthMetrics()
                healthMetrics.value = metrics
            } catch (e: Exception) {
                healthMetrics.value = emptyMap()
            }
        }

        return healthMetrics.asStateFlow()
    }
}