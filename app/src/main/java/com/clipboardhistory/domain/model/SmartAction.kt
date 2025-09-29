package com.clipboardhistory.domain.model

/** Minimal model representing a smart action with a label and target state. */
data class SmartAction(
    val label: String,
    val action: BubbleState,
)
