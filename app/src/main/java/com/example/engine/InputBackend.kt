package com.example.engine

import android.graphics.PointF

interface InputBackend {
    val backendName: String

    suspend fun dispatchTap(x: Float, y: Float, durationMs: Long = 50L): Boolean
    suspend fun dispatchSwipe(points: List<PointF>, durationMs: Long = 300L): Boolean
    suspend fun dispatchHoldSegment(x: Float, y: Float, isInitial: Boolean, willContinue: Boolean): Boolean
    suspend fun releaseHold(x: Float, y: Float): Boolean
    fun stopAll()
}

/**
 * MockBackend: Used for in-memory testing, preview, and unit tests.
 * Logs gesture actions without calling Android framework AccessibilityService.
 */
class MockBackend : InputBackend {
    override val backendName: String = "MockBackend (Test/Preview)"

    private val _dispatchedGestures = mutableListOf<String>()
    val dispatchedGestures: List<String> get() = _dispatchedGestures

    override suspend fun dispatchTap(x: Float, y: Float, durationMs: Long): Boolean {
        _dispatchedGestures.add("TAP at ($x, $y) for ${durationMs}ms")
        return true
    }

    override suspend fun dispatchSwipe(points: List<PointF>, durationMs: Long): Boolean {
        _dispatchedGestures.add("SWIPE across ${points.size} points for ${durationMs}ms")
        return true
    }

    override suspend fun dispatchHoldSegment(x: Float, y: Float, isInitial: Boolean, willContinue: Boolean): Boolean {
        _dispatchedGestures.add("HOLD segment at ($x, $y) - init=$isInitial, continue=$willContinue")
        return true
    }

    override suspend fun releaseHold(x: Float, y: Float): Boolean {
        _dispatchedGestures.add("RELEASE hold at ($x, $y)")
        return true
    }

    override fun stopAll() {
        _dispatchedGestures.add("STOP ALL")
    }

    fun clearLog() {
        _dispatchedGestures.clear()
    }
}

/**
 * Future ShizukuBackend interface placeholder (Phase 6/Future)
 */
interface ShizukuBackend : InputBackend {
    val isShizukuAvailable: Boolean
}
