package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Composable content for calculator bubbles.
 * Provides quick calculation capabilities using clipboard content or manual input.
 */
@Composable
fun CalculatorBubbleContent(
    initialExpression: String = "",
    onResultCalculated: (String) -> Unit = {}
) {
    var expression by remember { mutableStateOf(initialExpression) }
    var result by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "calculator_content"
        ) { expanded ->
            if (expanded) {
                ExpandedCalculator(
                    expression = expression,
                    result = result,
                    onExpressionChange = { expression = it },
                    onCalculate = {
                        result = calculateExpression(expression)
                        result?.let { onResultCalculated(it) }
                    },
                    onCollapse = { isExpanded = false },
                    onClear = {
                        expression = ""
                        result = null
                    }
                )
            } else {
                CollapsedCalculator(
                    expression = expression,
                    result = result,
                    onExpand = { isExpanded = true }
                )
            }
        }
    }
}

/**
 * Collapsed calculator view showing result or basic info.
 */
@Composable
private fun CollapsedCalculator(
    expression: String,
    result: String?,
    onExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onExpand)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = "Calculator",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result ?: "Calculator",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            if (expression.isNotEmpty() && result == null) {
                Text(
                    text = expression.take(20) + if (expression.length > 20) "..." else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Icon(
            Icons.Default.ExpandMore,
            contentDescription = "Expand calculator",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Expanded calculator with full interface.
 */
@Composable
private fun ExpandedCalculator(
    expression: String,
    result: String?,
    onExpressionChange: (String) -> Unit,
    onCalculate: () -> Unit,
    onCollapse: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with collapse button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Calculator",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onCollapse, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.ExpandLess,
                    contentDescription = "Collapse calculator"
                )
            }
        }

        // Expression input
        OutlinedTextField(
            value = expression,
            onValueChange = onExpressionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter expression...") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        // Result display
        if (result != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.End
                )
            }
        }

        // Quick operation buttons
        CalculatorButtons(
            onButtonClick = { button ->
                when (button) {
                    "=" -> onCalculate()
                    "C" -> onClear()
                    else -> onExpressionChange(expression + button)
                }
            }
        )
    }
}

/**
 * Calculator button grid.
 */
@Composable
private fun CalculatorButtons(onButtonClick: (String) -> Unit) {
    val buttons = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "=", "+"),
        listOf("C", "(", ")", "^")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        buttons.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { button ->
                    CalculatorButton(
                        text = button,
                        onClick = { onButtonClick(button) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Individual calculator button.
 */
@Composable
private fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOperator = text in listOf("/", "*", "-", "+", "=", "^")
    val isSpecial = text in listOf("C", "(", ")")

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = when {
            isOperator -> MaterialTheme.colorScheme.secondaryContainer
            isSpecial -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
                    isSpecial -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Simple expression calculator.
 * Supports basic arithmetic operations and parentheses.
 */
private fun calculateExpression(expression: String): String? {
    return try {
        // Remove any whitespace
        val cleanExpression = expression.replace("\\s".toRegex(), "")

        if (cleanExpression.isEmpty()) return null

        // Simple evaluation - in a real app, you'd want a proper expression parser
        val result = evaluateSimpleExpression(cleanExpression)

        // Format result (remove .0 for whole numbers)
        if (result % 1.0 == 0.0) {
            result.toInt().toString()
        } else {
            String.format("%.6f", result).trimEnd('0').trimEnd('.')
        }
    } catch (e: Exception) {
        "Error"
    }
}

/**
 * Simple expression evaluator for basic arithmetic.
 * This is a basic implementation - production code would use a proper math parser.
 */
private fun evaluateSimpleExpression(expression: String): Double {
    // Handle parentheses first (basic implementation)
    val parenRegex = "\\(([^()]+)\\)".toRegex()
    var expr = expression

    while (parenRegex.containsMatchIn(expr)) {
        expr = parenRegex.replace(expr) { match ->
            val inner = match.groupValues[1]
            evaluateSimpleExpression(inner).toString()
        }
    }

    // Handle power operations (^)
    while ("^" in expr) {
        val powerRegex = "([0-9.]+)\\^([0-9.]+)".toRegex()
        expr = powerRegex.replace(expr) { match ->
            val base = match.groupValues[1].toDouble()
            val exponent = match.groupValues[2].toDouble()
            pow(base, exponent).toString()
        }
    }

    // Handle multiplication and division
    while ("*" in expr || "/" in expr) {
        val mdRegex = "([0-9.]+)([*/])([0-9.]+)".toRegex()
        expr = mdRegex.replace(expr) { match ->
            val left = match.groupValues[1].toDouble()
            val op = match.groupValues[2]
            val right = match.groupValues[3].toDouble()

            when (op) {
                "*" -> (left * right).toString()
                "/" -> (left / right).toString()
                else -> match.value
            }
        }
    }

    // Handle addition and subtraction
    while ("+" in expr || "-" in expr) {
        // Handle negative numbers at start
        if (expr.startsWith("-")) {
            val parts = expr.substring(1).split("(?=[-+])".toRegex())
            if (parts.isNotEmpty()) {
                return -evaluateSimpleExpression(parts[0]) + evaluateSimpleExpression(parts.drop(1).joinToString(""))
            }
        }

        val asRegex = "([0-9.]+)([+-])([0-9.]+)".toRegex()
        val match = asRegex.find(expr)

        if (match != null) {
            val left = match.groupValues[1].toDouble()
            val op = match.groupValues[2]
            val rightStr = expr.substring(match.range.last + 1)

            val right = if (rightStr.isNotEmpty()) {
                evaluateSimpleExpression(rightStr)
            } else {
                0.0
            }

            return when (op) {
                "+" -> left + right
                "-" -> left - right
                else -> left
            }
        } else {
            break
        }
    }

    // If no operators found, try to parse as number
    return expr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid expression")
}