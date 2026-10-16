package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TouchAutomationService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Honest disclosure: We do NOT inspect screen content or read user text.
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        instance = null
    }

    suspend fun performTap(x: Float, y: Float, durationMs: Long): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(10L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureSuspending(gesture)
    }

    suspend fun performSwipe(points: List<PointF>, durationMs: Long): Boolean {
        if (points.isEmpty()) return false
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(50L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureSuspending(gesture)
    }

    suspend fun performHoldSegment(
        x: Float,
        y: Float,
        durationMs: Long,
        willContinue: Boolean
    ): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            GestureDescription.StrokeDescription(path, 0, durationMs, willContinue)
        } else {
            GestureDescription.StrokeDescription(path, 0, durationMs)
        }
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureSuspending(gesture)
    }

    private suspend fun dispatchGestureSuspending(gesture: GestureDescription): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val success = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    deferred.complete(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    deferred.complete(false)
                }
            },
            null
        )
        if (!success) return false
        return try {
            deferred.await()
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        var instance: TouchAutomationService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()
    }
}
