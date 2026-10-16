package com.example.engine

import android.graphics.PointF
import com.example.service.TouchAutomationService

class AccessibilityBackend : InputBackend {
    override val backendName: String = "AccessibilityService (dispatchGesture)"

    override suspend fun dispatchTap(x: Float, y: Float, durationMs: Long): Boolean {
        val service = TouchAutomationService.instance ?: return false
        return service.performTap(x, y, durationMs)
    }

    override suspend fun dispatchSwipe(points: List<PointF>, durationMs: Long): Boolean {
        val service = TouchAutomationService.instance ?: return false
        return service.performSwipe(points, durationMs)
    }

    override suspend fun dispatchHoldSegment(
        x: Float,
        y: Float,
        isInitial: Boolean,
        willContinue: Boolean
    ): Boolean {
        val service = TouchAutomationService.instance ?: return false
        return service.performHoldSegment(x, y, 100L, willContinue)
    }

    override suspend fun releaseHold(x: Float, y: Float): Boolean {
        val service = TouchAutomationService.instance ?: return false
        return service.performHoldSegment(x, y, 20L, willContinue = false)
    }

    override fun stopAll() {
        // AccessibilityService cancels ongoing gestures when service disconnects or new gestures are refused
    }
}
