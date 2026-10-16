package com.example

import com.example.engine.TouchDetector
import com.example.engine.TouchDetectorConfig
import com.example.model.ButtonBehaviorType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TouchDetectorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testShortTapDetection() = testScope.runTest {
        val detected = mutableListOf<ButtonBehaviorType>()
        val detector = TouchDetector(
            config = TouchDetectorConfig(
                longPressThresholdMs = 500L,
                doubleTapIntervalMs = 300L,
                isShortTapEnabled = true,
                isDoubleTapEnabled = false
            ),
            scope = this,
            onBehaviorDetected = { detected.add(it) }
        )

        detector.onTouchDown()
        testDispatcher.scheduler.advanceTimeBy(50)
        detector.onTouchUp()

        assertEquals(listOf(ButtonBehaviorType.RELEASE, ButtonBehaviorType.SHORT_TAP), detected)
    }

    @Test
    fun testLongPressDetection() = testScope.runTest {
        val detected = mutableListOf<ButtonBehaviorType>()
        val detector = TouchDetector(
            config = TouchDetectorConfig(
                longPressThresholdMs = 400L,
                doubleTapIntervalMs = 250L,
                isLongPressEnabled = true
            ),
            scope = this,
            onBehaviorDetected = { detected.add(it) }
        )

        detector.onTouchDown()
        testDispatcher.scheduler.advanceTimeBy(450)
        detector.onTouchUp()

        assertTrue(detected.contains(ButtonBehaviorType.LONG_PRESS))
        assertTrue(detected.contains(ButtonBehaviorType.RELEASE))
    }
}
