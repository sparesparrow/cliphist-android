package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.runtime.Composable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.utils.ContentAnalysisResult
import com.clipboardhistory.utils.ContentInsight
import com.clipboardhistory.utils.SuggestedAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Composable content for AI Assistant bubbles.
 * Displays intelligent content analysis, insights, and suggested actions.
 */
@Composable
fun AIAssistantBubbleContent(spec: AIAssistantBubble) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(AITab.INSIGHTS) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(300))
            },
            label = "ai_assistant_content"
        ) { expanded ->
            if (expanded) {
                ExpandedAIAssistant(spec, selectedTab) { selectedTab = it }
            } else {
                CollapsedAIAssistant(spec) { isExpanded = true }
            }
        }
    }
}

/**
 * Collapsed AI Assistant view showing status and quick analysis.
 */
@Composable
private fun CollapsedAIAssistant(
    spec: AIAssistantBubble,
    onExpand: () -> Unit
) {
    val status = spec.getAnalysisStatus()
    val hasInsights = spec.analysisResult?.insights?.isNotEmpty() == true
    val topAction = spec.getTopSuggestedActions(1).firstOrNull()

    // Dynamic background based on status
    val backgroundColor = when (status) {
        AnalysisStatus.ANALYZING -> Color(0xFF2196F3) // Blue - analyzing
        AnalysisStatus.COMPLETED -> if (hasInsights) Color(0xFF4CAF50) else Color(0xFFFF9800) // Green or Orange
        AnalysisStatus.HAS_HISTORY -> Color(0xFF9C27B0) // Purple - has history
        AnalysisStatus.IDLE -> MaterialTheme.colorScheme.primary
    }

    val animatedColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(500),
        label = "ai_status_color"
    )

    Surface(
        modifier = Modifier
            .size(if (status == AnalysisStatus.ANALYZING) 100.dp else 80.dp)
            .clickable(onClick = onExpand),
        shape = CircleShape,
        color = animatedColor,
        shadowElevation = if (status == AnalysisStatus.ANALYZING) 12.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (status) {
                AnalysisStatus.ANALYZING -> AnalyzingAnimation()
                AnalysisStatus.COMPLETED -> {
                    if (hasInsights) {
                        InsightsIcon(spec.analysisResult?.insights?.size ?: 0)
                    } else {
                        CompletedIcon()
                    }
                }
                AnalysisStatus.HAS_HISTORY -> HistoryIcon(spec.analysisHistory.size)
                AnalysisStatus.IDLE -> IdleIcon()
            }

            // Quick action indicator
            if (topAction != null && status == AnalysisStatus.COMPLETED) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Icons for different AI states.
 */
@Composable
private fun AnalyzingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "analyzing_rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "analyzing_scale"
    )

    Icon(
        Icons.Default.Psychology,
        contentDescription = "AI analyzing",
        tint = Color.White,
        modifier = Modifier
            .size(32.dp)
            .rotate(rotation)
            .scale(scale)
    )
}

@Composable
private fun InsightsIcon(insightCount: Int) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.Lightbulb,
            contentDescription = "AI insights available",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )

        if (insightCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = insightCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedIcon() {
    Icon(
        Icons.Default.CheckCircle,
        contentDescription = "Analysis completed",
        tint = Color.White,
        modifier = Modifier.size(28.dp)
    )
}

@Composable
private fun HistoryIcon(historyCount: Int) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.History,
            contentDescription = "Analysis history available",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(14.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = historyCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 7.sp
                )
            }
        }
    }
}

@Composable
private fun IdleIcon() {
    Icon(
        Icons.Default.SmartToy,
        contentDescription = "AI Assistant ready",
        tint = Color.White,
        modifier = Modifier.size(24.dp)
    )
}

/**
 * Expanded AI Assistant view with full analysis and controls.
 */
@Composable
private fun ExpandedAIAssistant(
    spec: AIAssistantBubble,
    selectedTab: AITab,
    onTabSelected: (AITab) -> Unit
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .heightIn(min = 400.dp, max = 600.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with title and status
        AIAssistantHeader(spec)

        // Tab selector
        AITabSelector(selectedTab, onTabSelected)

        // Tab content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                    fadeOut(animationSpec = tween(200))
            },
            label = "ai_tab_content"
        ) { tab ->
            when (tab) {
                AITab.INSIGHTS -> InsightsTab(spec)
                AITab.ACTIONS -> ActionsTab(spec)
                AITab.HISTORY -> HistoryTab(spec)
                AITab.SETTINGS -> SettingsTab(spec)
            }
        }
    }
}

/**
 * Header with AI status and controls.
 */
@Composable
private fun AIAssistantHeader(spec: AIAssistantBubble) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Status indicator
        val status = spec.getAnalysisStatus()
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when (status) {
                AnalysisStatus.ANALYZING -> MaterialTheme.colorScheme.primaryContainer
                AnalysisStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                AnalysisStatus.HAS_HISTORY -> MaterialTheme.colorScheme.tertiaryContainer
                AnalysisStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = when (status) {
                    AnalysisStatus.ANALYZING -> "Analyzing..."
                    AnalysisStatus.COMPLETED -> "Ready"
                    AnalysisStatus.HAS_HISTORY -> "History"
                    AnalysisStatus.IDLE -> "Idle"
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = when (status) {
                    AnalysisStatus.ANALYZING -> MaterialTheme.colorScheme.onPrimaryContainer
                    AnalysisStatus.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
                    AnalysisStatus.HAS_HISTORY -> MaterialTheme.colorScheme.onTertiaryContainer
                    AnalysisStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Tab selector for different AI views.
 */
@Composable
private fun AITabSelector(selectedTab: AITab, onTabSelected: (AITab) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(AITab.values()) { tab ->
            TabButton(
                tab = tab,
                isSelected = tab == selectedTab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

/**
 * Individual tab button.
 */
@Composable
private fun TabButton(tab: AITab, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                tab.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = tab.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Insights tab showing analysis results.
 */
@Composable
private fun InsightsTab(spec: AIAssistantBubble) {
    val analysis = spec.analysisResult

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (analysis == null) {
            // No analysis available
            EmptyInsightsView()
        } else {
            // Analysis summary
            AnalysisSummary(analysis)

            // Insights list
            if (analysis.insights.isNotEmpty()) {
                InsightsList(analysis.insights)
            }

            // Content preview
            ContentPreview(analysis.originalContent)
        }
    }
}

/**
 * Actions tab showing suggested actions.
 */
@Composable
private fun ActionsTab(spec: AIAssistantBubble) {
    val actions = spec.getTopSuggestedActions()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (actions.isEmpty()) {
            EmptyActionsView()
        } else {
            Text(
                text = "Suggested Actions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            ActionsList(actions)
        }
    }
}

/**
 * History tab showing analysis history.
 */
@Composable
private fun HistoryTab(spec: AIAssistantBubble) {
    val history = spec.analysisHistory

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (history.isEmpty()) {
            EmptyHistoryView()
        } else {
            Text(
                text = "Analysis History",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            HistoryList(history)
        }
    }
}

/**
 * Settings tab for AI configuration.
 */
@Composable
private fun SettingsTab(spec: AIAssistantBubble) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "AI Features",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Feature toggles
        AIFeature.values().forEach { feature ->
            FeatureToggle(
                feature = feature,
                isEnabled = spec.isFeatureEnabled(feature),
                onToggle = { enabled ->
                    // This would be handled by the ViewModel
                }
            )
        }

        // Statistics
        val stats = spec.getAnalysisStats()
        if (stats.totalAnalyses > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            AnalysisStatistics(stats)
        }
    }
}

/**
 * Empty states for different tabs.
 */
@Composable
private fun EmptyInsightsView() {
    EmptyState(
        icon = Icons.Default.Lightbulb,
        title = "No Analysis Yet",
        description = "Add content to clipboard for AI analysis"
    )
}

@Composable
private fun EmptyActionsView() {
    EmptyState(
        icon = Icons.Default.TouchApp,
        title = "No Actions Available",
        description = "Analyze content to see suggested actions"
    )
}

@Composable
private fun EmptyHistoryView() {
    EmptyState(
        icon = Icons.Default.History,
        title = "No History",
        description = "Analysis history will appear here"
    )
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Analysis summary card.
 */
@Composable
private fun AnalysisSummary(analysis: ContentAnalysisResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = analysis.contentType.name.replace("_", " "),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Confidence indicator
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when {
                        analysis.confidence >= 0.8f -> Color(0xFF4CAF50)
                        analysis.confidence >= 0.6f -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                ) {
                    Text(
                        text = "${(analysis.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (analysis.entities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Found ${analysis.entities.size} entities",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * List of insights.
 */
@Composable
private fun InsightsList(insights: List<ContentInsight>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.heightIn(max = 150.dp)
    ) {
        items(insights) { insight ->
            InsightItem(insight)
        }
    }
}

/**
 * Individual insight item.
 */
@Composable
private fun InsightItem(insight: ContentInsight) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Priority indicator
                Surface(
                    shape = CircleShape,
                    color = when (insight.priority) {
                        com.clipboardhistory.utils.InsightPriority.HIGH -> Color(0xFFFF9800)
                        com.clipboardhistory.utils.InsightPriority.CRITICAL -> Color(0xFFF44336)
                        else -> Color(0xFF4CAF50)
                    }
                ) {
                    Box(modifier = Modifier.size(6.dp))
                }
            }

            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Content preview.
 */
@Composable
private fun ContentPreview(content: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = content.take(200) + if (content.length > 200) "..." else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * List of suggested actions.
 */
@Composable
private fun ActionsList(actions: List<SuggestedAction>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.heightIn(max = 200.dp)
    ) {
        items(actions) { action ->
            ActionItem(action)
        }
    }
}

/**
 * Individual action item.
 */
@Composable
private fun ActionItem(action: SuggestedAction) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle action */ },
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Action icon (placeholder - would use actual icons)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = action.icon.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Execute action",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Analysis history list.
 */
@Composable
private fun HistoryList(history: List<AnalysisRecord>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.heightIn(max = 200.dp)
    ) {
        items(history) { record ->
            HistoryItem(record)
        }
    }
}

/**
 * Individual history item.
 */
@Composable
private fun HistoryItem(record: AnalysisRecord) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = record.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Confidence badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when {
                        record.confidence >= 0.8f -> Color(0xFF4CAF50)
                        record.confidence >= 0.6f -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                ) {
                    Text(
                        text = "${(record.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.contentType.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = formatTimestamp(record.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.insights.isNotEmpty()) {
                Text(
                    text = "${record.insights.size} insights, ${record.actions.size} actions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Feature toggle for settings.
 */
@Composable
private fun FeatureToggle(feature: AIFeature, isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

/**
 * Analysis statistics display.
 */
@Composable
private fun AnalysisStatistics(stats: AnalysisStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Analysis Statistics",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                StatItem("Total", stats.totalAnalyses.toString())
                StatItem("Avg Confidence", "${(stats.averageConfidence * 100).toInt()}%")
                StatItem("History", stats.analysisHistorySize.toString())
            }

            if (stats.contentTypeDistribution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Content Types: ${stats.contentTypeDistribution.entries.joinToString { "${it.key.name.lowercase()}: ${it.value}" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Statistic item.
 */
@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * AI tabs for different views.
 */
enum class AITab(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    INSIGHTS("Insights", Icons.Default.Lightbulb),
    ACTIONS("Actions", Icons.Default.TouchApp),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings)
}

/**
 * AI feature descriptions.
 */
val AIFeature.displayName: String
    get() = when (this) {
        AIFeature.CONTENT_ANALYSIS -> "Content Analysis"
        AIFeature.INSIGHT_GENERATION -> "Insight Generation"
        AIFeature.ACTION_SUGGESTIONS -> "Action Suggestions"
        AIFeature.AUTOMATED_ACTIONS -> "Automated Actions"
        AIFeature.LEARNING_ADAPTATION -> "Learning Adaptation"
        AIFeature.CONTEXT_AWARENESS -> "Context Awareness"
    }

val AIFeature.description: String
    get() = when (this) {
        AIFeature.CONTENT_ANALYSIS -> "Analyze clipboard content type and structure"
        AIFeature.INSIGHT_GENERATION -> "Generate insights about content meaning and context"
        AIFeature.ACTION_SUGGESTIONS -> "Suggest relevant actions based on content analysis"
        AIFeature.AUTOMATED_ACTIONS -> "Automatically perform routine actions"
        AIFeature.LEARNING_ADAPTATION -> "Learn from user behavior and preferences"
        AIFeature.CONTEXT_AWARENESS -> "Consider app context and usage history"
    }

// Helper function
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}