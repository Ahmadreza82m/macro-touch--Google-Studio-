package com.example.engine

import android.os.SystemClock
import com.example.model.ButtonBehaviorType
import kotlinx.coroutines.*

data class TouchDetectorConfig(
    val longPressThresholdMs: Long = 500L,
    val doubleTapIntervalMs: Long = 300L,
    val isShortTapEnabled: Boolean = true,
    val isDoubleTapEnabled: Boolean = true,
    val isLongPressEnabled: Boolean = true,
    val isReleaseEnabled: Boolean = true
)

class TouchDetector(
    var config: TouchDetectorConfig = TouchDetectorConfig(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val onBehaviorDetected: (ButtonBehaviorType) -> Unit
) {
    private var downTimestamp = 0L
    private var lastTapTimestamp = 0L
    private var tapPendingJob: Job? = null
    private var longPressPendingJob: Job? = null
    private var isLongPressFired = false

    fun onTouchDown() {
        downTimestamp = SystemClock.elapsedRealtime()
        isLongPressFired = false

        if (config.isLongPressEnabled) {
            longPressPendingJob?.cancel()
            longPressPendingJob = scope.launch {
                delay(config.longPressThresholdMs)
                isLongPressFired = true
                onBehaviorDetected(ButtonBehaviorType.LONG_PRESS)
            }
        }
    }

    fun onTouchUp() {
        val upTimestamp = SystemClock.elapsedRealtime()
        longPressPendingJob?.cancel()
        longPressPendingJob = null

        if (config.isReleaseEnabled) {
            onBehaviorDetected(ButtonBehaviorType.RELEASE)
        }

        if (isLongPressFired) {
            return
        }

        val pressDuration = upTimestamp - downTimestamp
        if (pressDuration >= config.longPressThresholdMs) {
            return
        }

        // Tap handling with double tap detection
        val timeSinceLastTap = upTimestamp - lastTapTimestamp

        if (config.isDoubleTapEnabled && timeSinceLastTap <= config.doubleTapIntervalMs) {
            tapPendingJob?.cancel()
            tapPendingJob = null
            lastTapTimestamp = 0L
            onBehaviorDetected(ButtonBehaviorType.DOUBLE_TAP)
        } else {
            lastTapTimestamp = upTimestamp

            if (config.isDoubleTapEnabled) {
                // With double-tap enabled, wait for the window before confirming short tap
                tapPendingJob?.cancel()
                tapPendingJob = scope.launch {
                    delay(config.doubleTapIntervalMs)
                    if (config.isShortTapEnabled) {
                        onBehaviorDetected(ButtonBehaviorType.SHORT_TAP)
                    }
                }
            } else {
                // Immediate short tap if double tap is disabled
                if (config.isShortTapEnabled) {
                    onBehaviorDetected(ButtonBehaviorType.SHORT_TAP)
                }
            }
        }
    }

    fun reset() {
        tapPendingJob?.cancel()
        longPressPendingJob?.cancel()
        tapPendingJob = null
        longPressPendingJob = null
        isLongPressFired = false
        lastTapTimestamp = 0L
    }
}
