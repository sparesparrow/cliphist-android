package com.clipboardhistory.domain.model

import com.clipboardhistory.domain.model.BubbleState

/** Minimal model representing a smart action with a label and target state. */
data class SmartAction(
    val label: String,
    val action: BubbleState
)

