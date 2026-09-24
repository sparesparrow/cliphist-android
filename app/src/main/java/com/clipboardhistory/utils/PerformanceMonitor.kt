package com.clipboardhistory.utils

import java.util.ArrayDeque

object PerformanceMonitor {

    private const val MAX_ENTRIES_PER_OPERATION = 100

    private val data: MutableMap<String, ArrayDeque<Long>> = mutableMapOf()

    @Synchronized
    fun record(operationName: String, durationMs: Long) {
        val queue = data.getOrPut(operationName) { ArrayDeque(MAX_ENTRIES_PER_OPERATION + 1) }
        queue.addLast(durationMs)
        if (queue.size > MAX_ENTRIES_PER_OPERATION) {
            queue.removeFirst()
        }
    }

    @Synchronized
    fun getMetrics(operationName: String): OperationMetrics {
        val samples = data[operationName]
        if (samples.isNullOrEmpty()) {
            return OperationMetrics(operationName, 0, 0L, 0L, 0.0)
        }
        val list = samples.toList()
        return OperationMetrics(
            operationName = operationName,
            sampleCount = list.size,
            minMs = list.min(),
            maxMs = list.max(),
            avgMs = list.average(),
        )
    }

    @Synchronized
    fun getSlowOperations(thresholdMs: Long = 200L): List<Pair<String, OperationMetrics>> {
        return data.keys
            .map { name -> name to getMetrics(name) }
            .filter { (_, metrics) -> metrics.sampleCount > 0 && metrics.avgMs > thresholdMs }
    }

    @Synchronized
    fun reset() {
        data.clear()
    }
}

data class OperationMetrics(
    val operationName: String,
    val sampleCount: Int,
    val minMs: Long,
    val maxMs: Long,
    val avgMs: Double,
)
