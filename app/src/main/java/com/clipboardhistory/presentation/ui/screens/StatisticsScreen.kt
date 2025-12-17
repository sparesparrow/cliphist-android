package com.clipboardhistory.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clipboardhistory.domain.repository.ClipboardStatistics
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import com.clipboardhistory.presentation.viewmodels.StatisticsViewModel
import kotlinx.coroutines.launch

/**
 * Statistics dashboard showing clipboard usage analytics and insights.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Use dedicated statistics view model
    val statisticsViewModel: StatisticsViewModel = hiltViewModel()
    val statistics by statisticsViewModel.statistics.collectAsState()
    val efficiencyMetrics by statisticsViewModel.efficiencyMetrics.collectAsState()
    val contentTypeDistribution by statisticsViewModel.contentTypeDistribution.collectAsState()
    val hourlyActivity by statisticsViewModel.hourlyActivity.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                statisticsViewModel.refreshStatistics()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Overview Cards
            if (statistics != null) {
                StatisticsOverviewCards(statistics!!)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Content Type Distribution
            if (contentTypeDistribution.isNotEmpty()) {
                ContentTypeDistributionCard(contentTypeDistribution)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Activity Patterns
            if (hourlyActivity.isNotEmpty()) {
                ActivityPatternsCard(hourlyActivity)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Efficiency Metrics
            if (efficiencyMetrics.isNotEmpty()) {
                EfficiencyMetricsCard(efficiencyMetrics)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Insights Section
            InsightsCard(statistics)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatisticsOverviewCards(statistics: ClipboardStatistics) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatisticsCard(
            title = "Total Items",
            value = statistics.totalItems.toString(),
            icon = Icons.Default.Storage,
            modifier = Modifier.weight(1f),
        )

        StatisticsCard(
            title = "Favorites",
            value = statistics.favoriteItems.toString(),
            icon = Icons.Default.Favorite,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatisticsCard(
            title = "Today",
            value = statistics.itemsToday.toString(),
            icon = Icons.Default.DateRange,
            modifier = Modifier.weight(1f),
        )

        StatisticsCard(
            title = "This Week",
            value = statistics.itemsThisWeek.toString(),
            icon = Icons.Default.Schedule,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatisticsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContentTypeDistributionCard(distribution: Map<String, Int>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Content Types",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            distribution.entries
                .sortedByDescending { it.value }
                .take(5) // Show top 5
                .forEach { (type, count) ->
                    ContentTypeRow(
                        type = type,
                        count = count,
                        total = distribution.values.sum(),
                    )
                }
        }
    }
}

@Composable
private fun ContentTypeRow(type: String, count: Int, total: Int) {
    val percentage = if (total > 0) (count.toFloat() / total * 100).toInt() else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatContentType(type),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$count ($percentage%)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivityPatternsCard(hourlyActivity: Map<Int, Int>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            val maxActivity = hourlyActivity.values.maxOrNull() ?: 1
            val mostActiveHour = hourlyActivity.maxByOrNull { it.value }?.key

            Text(
                text = "Most active hour: ${mostActiveHour?.let { formatHour(it) } ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Simple bar chart representation
            hourlyActivity.entries
                .sortedBy { it.key }
                .forEach { (hour, count) ->
                    ActivityBar(
                        hour = hour,
                        count = count,
                        maxCount = maxActivity,
                    )
                }
        }
    }
}

@Composable
private fun ActivityBar(hour: Int, count: Int, maxCount: Int) {
    val barWidth = if (maxCount > 0) (count.toFloat() / maxCount * 200).dp else 0.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatHour(hour),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .height(8.dp)
                .width(barWidth)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EfficiencyMetricsCard(metrics: Map<String, Any>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Efficiency Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            metrics.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatMetricKey(key),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatMetricValue(value),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsCard(statistics: ClipboardStatistics?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (statistics != null) {
                val insights = generateInsights(statistics)
                insights.forEach { insight ->
                    Text(
                        text = "• $insight",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            } else {
                Text(
                    text = "Loading insights...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Helper functions

private fun formatContentType(type: String): String {
    return when (type) {
        "text/plain" -> "Plain Text"
        "text/url" -> "URLs"
        "text/email" -> "Emails"
        "text/phone" -> "Phone Numbers"
        "text/address" -> "Addresses"
        "application/json" -> "JSON"
        "text/credit-card" -> "Credit Cards"
        else -> type.replaceFirstChar { it.uppercase() }
    }
}

private fun formatHour(hour: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when (hour) {
        0 -> 12
        in 1..12 -> hour
        else -> hour - 12
    }
    return "${displayHour}$period"
}

private fun formatMetricKey(key: String): String {
    return when (key) {
        "itemsToday" -> "Items Today"
        "duplicateCount" -> "Duplicates"
        "averageItemsPerDay" -> "Avg/Day"
        "uniqueContentRatio" -> "Unique Ratio"
        "mostProductiveHour" -> "Peak Hour"
        else -> key.replaceFirstChar { it.uppercase() }
    }
}

private fun formatMetricValue(value: Any): String {
    return when (value) {
        is Float -> String.format("%.1f", value)
        is Double -> String.format("%.2f", value)
        is Int -> {
            when {
                value < 10 -> value.toString()
                value < 100 -> value.toString()
                else -> "${value / 1000}K"
            }
        }
        else -> value.toString()
    }
}

private fun generateInsights(statistics: ClipboardStatistics): List<String> {
    val insights = mutableListOf<String>()

    // Activity insights
    if (statistics.itemsToday > statistics.itemsThisWeek / 7) {
        insights.add("You're more active today than your weekly average!")
    }

    if (statistics.favoriteItems > statistics.totalItems * 0.1) {
        insights.add("You have many favorites - consider organizing them!")
    }

    // Content type insights
    when (statistics.mostUsedContentType) {
        "text/url" -> insights.add("You copy many URLs - try using bookmarks for frequently visited sites")
        "text/plain" -> insights.add("Most of your copies are plain text - consider using templates")
        "text/phone" -> insights.add("You copy phone numbers often - your contacts app might be useful")
    }

    // Productivity insights
    if (statistics.averageContentLength < 20) {
        insights.add("Your average content is short - consider copying more complete information")
    }

    if (insights.isEmpty()) {
        insights.add("Keep up the good work! Your clipboard usage looks healthy.")
    }

    return insights.take(3) // Limit to 3 insights
}