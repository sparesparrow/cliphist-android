package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipboardhistory.presentation.ui.bubble.AdvancedBubbleSpec.SearchBubble
import com.clipboardhistory.presentation.ui.bubble.SearchFilter

/**
 * Composable content for search bubbles.
 * Provides a search interface for clipboard history with filters and suggestions.
 */
@Composable
fun SearchBubbleContent(spec: SearchBubble) {
    var searchQuery by remember { mutableStateOf(spec.searchQuery) }
    var selectedFilters by remember { mutableStateOf(spec.searchFilters) }
    var isFilterExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .heightIn(min = 200.dp, max = 400.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with search icon and expand/collapse
            SearchBubbleHeader(
                onFilterToggle = { isFilterExpanded = !isFilterExpanded },
                onClearSearch = { searchQuery = "" }
            )

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        // Auto-expand filters when search is focused
                        if (focusState.isFocused && !isFilterExpanded) {
                            isFilterExpanded = true
                        }
                    },
                placeholder = { Text("Search clipboard...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    // Perform search
                    performSearch(searchQuery, selectedFilters)
                }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter chips (expandable)
            AnimatedVisibility(
                visible = isFilterExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SearchFiltersChips(
                    selectedFilters = selectedFilters,
                    onFilterToggle = { filter ->
                        selectedFilters = if (filter in selectedFilters) {
                            selectedFilters - filter
                        } else {
                            selectedFilters + filter
                        }
                    }
                )
            }

            // Search results or suggestions
            SearchResultsSection(
                searchQuery = searchQuery,
                selectedFilters = selectedFilters
            )
        }
    }
}

/**
 * Header with search controls.
 */
@Composable
private fun SearchBubbleHeader(
    onFilterToggle: () -> Unit,
    onClearSearch: () -> Unit
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
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Search Clipboard",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onFilterToggle) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Toggle filters"
                )
            }

            IconButton(onClick = onClearSearch) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Clear search"
                )
            }
        }
    }
}

/**
 * Filter selection chips.
 */
@Composable
private fun SearchFiltersChips(
    selectedFilters: Set<SearchFilter>,
    onFilterToggle: (SearchFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Filter chips in rows
        val filters = SearchFilter.values()
        val rows = filters.chunked(3) // 3 filters per row

        rows.forEach { rowFilters ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowFilters.forEach { filter ->
                    FilterChip(
                        selected = filter in selectedFilters,
                        onClick = { onFilterToggle(filter) },
                        label = {
                            Text(
                                text = filter.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fill remaining space if needed
                repeat(3 - rowFilters.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Search results or suggestions section.
 */
@Composable
private fun SearchResultsSection(
    searchQuery: String,
    selectedFilters: Set<SearchFilter>
) {
    val searchResults = remember(searchQuery, selectedFilters) {
        performSearch(searchQuery, selectedFilters)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (searchQuery.isEmpty()) {
            // Show suggestions when no search query
            SearchSuggestions()
        } else if (searchResults.isEmpty()) {
            // No results found
            SearchNoResults()
        } else {
            // Show search results
            SearchResultsList(searchResults)
        }
    }
}

/**
 * Search suggestions when no query is entered.
 */
@Composable
private fun SearchSuggestions() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quick Search",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val suggestions = listOf(
            "URLs" to SearchFilter.URLS_ONLY,
            "Images" to SearchFilter.IMAGES_ONLY,
            "Recent" to SearchFilter.RECENT_ONLY,
            "Favorites" to SearchFilter.FAVORITES_ONLY,
            "Code" to SearchFilter.CODE_ONLY
        )

        suggestions.forEach { (label, filter) ->
            SuggestionChip(label = label, filter = filter)
        }
    }
}

/**
 * Individual suggestion chip.
 */
@Composable
private fun SuggestionChip(label: String, filter: SearchFilter) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Apply filter */ },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                getFilterIcon(filter),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * No search results found.
 */
@Composable
private fun SearchNoResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = "No results found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Try adjusting your search or filters",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * List of search results.
 */
@Composable
private fun SearchResultsList(results: List<SearchResult>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(results) { result ->
            SearchResultItem(result)
        }
    }
}

/**
 * Individual search result item.
 */
@Composable
private fun SearchResultItem(result: SearchResult) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle result tap */ },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Content type icon
            Surface(
                shape = CircleShape,
                color = getContentTypeColor(result.contentType),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getContentTypeIcon(result.contentType),
                        contentDescription = result.contentType.name,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Content preview
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = result.previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (result.isFavorite) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Action button
            IconButton(onClick = { /* Quick action */ }) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy to clipboard"
                )
            }
        }
    }
}

// Helper functions

private fun performSearch(query: String, filters: Set<SearchFilter>): List<SearchResult> {
    // Placeholder implementation - in real app this would search actual clipboard data
    if (query.isEmpty()) return emptyList()

    return listOf(
        SearchResult(
            id = "1",
            previewText = "Sample search result for: $query",
            contentType = ContentType.TEXT,
            timestamp = "2 min ago",
            isFavorite = false
        )
    )
}

private val SearchFilter.displayName: String
    get() = when (this) {
        SearchFilter.TEXT_ONLY -> "Text"
        SearchFilter.URLS_ONLY -> "URLs"
        SearchFilter.IMAGES_ONLY -> "Images"
        SearchFilter.CODE_ONLY -> "Code"
        SearchFilter.RECENT_ONLY -> "Recent"
        SearchFilter.FAVORITES_ONLY -> "Favorites"
        SearchFilter.BY_DATE -> "By Date"
        SearchFilter.BY_APP -> "By App"
    }

private fun getFilterIcon(filter: SearchFilter) = when (filter) {
    SearchFilter.TEXT_ONLY -> Icons.Default.TextFields
    SearchFilter.URLS_ONLY -> Icons.Default.Link
    SearchFilter.IMAGES_ONLY -> Icons.Default.Image
    SearchFilter.CODE_ONLY -> Icons.Default.Code
    SearchFilter.RECENT_ONLY -> Icons.Default.Schedule
    SearchFilter.FAVORITES_ONLY -> Icons.Default.Star
    SearchFilter.BY_DATE -> Icons.Default.DateRange
    SearchFilter.BY_APP -> Icons.Default.Apps
}

private fun getContentTypeIcon(type: ContentType) = when (type) {
    ContentType.TEXT -> Icons.Default.TextFields
    ContentType.URL -> Icons.Default.Link
    ContentType.EMAIL -> Icons.Default.Email
    ContentType.PHONE_NUMBER -> Icons.Default.Phone
    ContentType.JSON -> Icons.Default.Code
    ContentType.XML -> Icons.Default.Code
    ContentType.CODE -> Icons.Default.Code
    ContentType.NUMBER -> Icons.Default.Calculate
    ContentType.IMAGE -> Icons.Default.Image
    ContentType.FILE_PATH -> Icons.Default.Folder
    ContentType.UNKNOWN -> Icons.Default.Help
}

private fun getContentTypeColor(type: ContentType) = when (type) {
    ContentType.TEXT -> Color(0xFF2196F3) // Blue
    ContentType.URL -> Color(0xFF4CAF50) // Green
    ContentType.EMAIL -> Color(0xFFFF9800) // Orange
    ContentType.PHONE_NUMBER -> Color(0xFF9C27B0) // Purple
    ContentType.JSON, ContentType.XML, ContentType.CODE -> Color(0xFF607D8B) // Blue Grey
    ContentType.NUMBER -> Color(0xFFFF5722) // Deep Orange
    ContentType.IMAGE -> Color(0xFFE91E63) // Pink
    ContentType.FILE_PATH -> Color(0xFF795548) // Brown
    ContentType.UNKNOWN -> Color(0xFF9E9E9E) // Grey
}

data class SearchResult(
    val id: String,
    val previewText: String,
    val contentType: ContentType,
    val timestamp: String,
    val isFavorite: Boolean
)