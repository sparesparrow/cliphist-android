package com.clipboardhistory.domain.voice

import kotlin.test.Test
import kotlin.test.assertTrue

class VoiceCommandEngineTest {
    @Test
    fun `contract compiles and can instantiate commands`() {
        val cmd: VoiceCommand = VoiceCommand.Search("find note")
        assertTrue(cmd is VoiceCommand)
    }
}

