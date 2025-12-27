package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.runtime.Composable
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.presentation.ui.bubble.AdvancedBubbleSpec.TemplateBubble
import com.clipboardhistory.presentation.ui.bubble.TextTemplate

/**
 * Composable content for template bubbles.
 * Provides categorized text templates and code snippets for insertion.
 */
@Composable
fun TemplateBubbleContent(spec: TemplateBubble) {
    var selectedCategory by remember { mutableStateOf(spec.selectedCategory ?: spec.categories.firstOrNull()) }
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .heightIn(min = 300.dp, max = 500.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with template icon and controls
            TemplateBubbleHeader(
                templateCount = spec.templates.size,
                onSearchToggle = { /* Could expand search */ },
                onCreateNew = { /* Open template creation */ }
            )

            // Category selector
            if (spec.categories.isNotEmpty()) {
                CategorySelector(
                    categories = spec.categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            // Templates list
            TemplateList(
                templates = getFilteredTemplates(spec.templates, selectedCategory, searchQuery),
                onTemplateSelected = { template ->
                    // Handle template insertion
                    insertTemplate(template)
                },
                onTemplateFavorite = { template ->
                    // Toggle favorite status
                    toggleTemplateFavorite(template)
                }
            )
        }
    }
}

/**
 * Header with template management controls.
 */
@Composable
private fun TemplateBubbleHeader(
    templateCount: Int,
    onSearchToggle: () -> Unit,
    onCreateNew: () -> Unit
) {
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
                Icons.Default.TextSnippet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = "Templates",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$templateCount available",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onSearchToggle) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search templates"
                )
            }

            IconButton(onClick = onCreateNew) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create new template"
                )
            }
        }
    }
}

/**
 * Horizontal category selector.
 */
@Composable
private fun CategorySelector(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // "All" category
        item {
            CategoryChip(
                category = "All",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected("") } // Empty string means "All"
            )
        }

        // Specific categories
        items(categories) { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * Individual category selection chip.
 */
@Composable
private fun CategoryChip(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Scrollable list of templates.
 */
@Composable
private fun TemplateList(
    templates: List<TextTemplate>,
    onTemplateSelected: (TextTemplate) -> Unit,
    onTemplateFavorite: (TextTemplate) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(templates) { template ->
            TemplateItem(
                template = template,
                onClick = { onTemplateSelected(template) },
                onFavoriteToggle = { onTemplateFavorite(template) }
            )
        }
    }
}

/**
 * Individual template item.
 */
@Composable
private fun TemplateItem(
    template: TextTemplate,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Template header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = template.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Usage count indicator
                    if (template.usageCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = template.usageCount.toString(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Favorite button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (template.tags.contains("favorite")) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (template.tags.contains("favorite")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Template content preview
            Text(
                text = template.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Tags (if any)
            if (template.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    template.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (template.tags.size > 3) {
                        Text(
                            text = "+${template.tags.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Helper functions

private fun getFilteredTemplates(
    allTemplates: List<TextTemplate>,
    selectedCategory: String?,
    searchQuery: String
): List<TextTemplate> {
    return allTemplates.filter { template ->
        // Category filter
        val categoryMatch = selectedCategory.isNullOrEmpty() ||
                selectedCategory == "All" ||
                template.category == selectedCategory

        // Search filter
        val searchMatch = searchQuery.isEmpty() ||
                template.name.contains(searchQuery, ignoreCase = true) ||
                template.content.contains(searchQuery, ignoreCase = true) ||
                template.tags.any { it.contains(searchQuery, ignoreCase = true) }

        categoryMatch && searchMatch
    }.sortedWith(
        compareByDescending<TextTemplate> { it.tags.contains("favorite") }
            .thenByDescending { it.usageCount }
            .thenByDescending { it.lastUsed }
    )
}

private fun insertTemplate(template: TextTemplate) {
    // In a real implementation, this would:
    // 1. Copy template content to clipboard
    // 2. Increment usage count
    // 3. Update last used timestamp
    // 4. Potentially paste directly if input field is focused

    println("Inserting template: ${template.name}")
    // TODO: Implement actual insertion logic
}

private fun toggleTemplateFavorite(template: TextTemplate) {
    // In a real implementation, this would:
    // 1. Toggle the "favorite" tag
    // 2. Update the template in storage
    // 3. Refresh the UI

    println("Toggling favorite for template: ${template.name}")
    // TODO: Implement favorite toggle logic
}

// Sample template data for demonstration
val sampleTemplates = listOf(
    TextTemplate(
        id = "1",
        name = "Email Signature",
        content = "Best regards,\n[Your Name]\n[Your Position]\n[Your Company]\n[Contact Information]",
        category = "Communication",
        tags = listOf("email", "signature", "professional"),
        usageCount = 25,
        lastUsed = System.currentTimeMillis()
    ),

    TextTemplate(
        id = "2",
        name = "Code Comment",
        content = "/**\n * [Brief description]\n *\n * @param [param] [description]\n * @return [description]\n */",
        category = "Development",
        tags = listOf("code", "comment", "documentation"),
        usageCount = 15,
        lastUsed = System.currentTimeMillis() - 3600000 // 1 hour ago
    ),

    TextTemplate(
        id = "3",
        name = "Meeting Notes",
        content = "# Meeting Notes - [Date]\n\n## Attendees\n- \n\n## Agenda\n- \n\n## Discussion\n- \n\n## Action Items\n- [ ] ",
        category = "Productivity",
        tags = listOf("meeting", "notes", "productivity", "favorite"),
        usageCount = 8,
        lastUsed = System.currentTimeMillis() - 86400000 // 1 day ago
    ),

    TextTemplate(
        id = "4",
        name = "JSON Response",
        content = """{
  "success": true,
  "data": {
    "id": "",
    "name": "",
    "description": ""
  },
  "message": ""
}""",
        category = "Development",
        tags = listOf("json", "api", "response"),
        usageCount = 12,
        lastUsed = System.currentTimeMillis() - 1800000 // 30 minutes ago
    ),

    TextTemplate(
        id = "5",
        name = "Bug Report",
        content = "## Bug Report\n\n**Title:** \n\n**Description:** \n\n**Steps to reproduce:**\n1. \n\n**Expected behavior:** \n\n**Actual behavior:** \n\n**Environment:**\n- OS: \n- Browser: \n- Version: ",
        category = "Development",
        tags = listOf("bug", "report", "documentation"),
        usageCount = 5,
        lastUsed = System.currentTimeMillis() - 7200000 // 2 hours ago
    )
)