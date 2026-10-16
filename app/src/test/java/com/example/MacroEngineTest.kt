package com.example

import android.graphics.PointF
import com.example.engine.MacroEngine
import com.example.engine.MacroEngineState
import com.example.engine.MockBackend
import com.example.model.Macro
import com.example.model.MacroStep
import com.example.model.StepType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MacroEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testMacroExecutionAndStop() = testScope.runTest {
        val mockBackend = MockBackend()
        val engine = MacroEngine(backend = mockBackend, scope = this)

        val macro = Macro(
            id = "test-macro-1",
            profileId = "test-profile-1",
            name = "Test Macro",
            repeatCount = 5,
            intervalMs = 100L
        )

        val step = MacroStep(
            id = "test-step-1",
            macroId = "test-macro-1",
            orderIndex = 1,
            stepType = StepType.TAP,
            gestureId = "gesture-1",
            delayMs = 50L
        )

        engine.startMacro(macro, listOf(step)) { _ ->
            Pair(listOf(PointF(100f, 200f)), 50L)
        }

        testDispatcher.scheduler.advanceTimeBy(80)

        // Stop engine
        engine.stop()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals(MacroEngineState.Idle, engine.engineState.value)
        assertTrue(mockBackend.dispatchedGestures.isNotEmpty())
    }
}
