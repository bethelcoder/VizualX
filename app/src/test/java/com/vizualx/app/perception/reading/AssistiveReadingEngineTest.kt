package com.vizualx.app.perception.reading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistiveReadingEngineTest {

    @Test
    fun `AssistiveReadingEngine initialization and state lifecycle`() {
        val engine = AssistiveReadingEngine()
        assertEquals(false, engine.isReading.value)
        assertEquals(null, engine.lastReadText.value)
        engine.close()
    }
}
