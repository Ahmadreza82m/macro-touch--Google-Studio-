package com.example.engine

import android.graphics.PointF
import android.os.SystemClock
import com.example.model.Macro
import com.example.model.MacroStep
import com.example.model.StepType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicLong

sealed class MacroEngineState {
    object Idle : MacroEngineState()
    data class Running(
        val macroName: String,
        val currentCycle: Int,
        val totalCycles: Int, // 0 = infinite
        val currentStepIndex: Int
    ) : MacroEngineState()
    object Paused : MacroEngineState()
    object Stopping : MacroEngineState()
    data class Error(val message: String) : MacroEngineState()
}

class MacroEngine(
    private var backend: InputBackend = MockBackend(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _engineState = MutableStateFlow<MacroEngineState>(MacroEngineState.Idle)
    val engineState: StateFlow<MacroEngineState> = _engineState.asStateFlow()

    private val mutex = Mutex()
    private var currentExecutionJob: Job? = null
    private val currentGeneration = AtomicLong(0)

    fun setBackend(newBackend: InputBackend) {
        backend = newBackend
    }

    fun startMacro(
        macro: Macro,
        steps: List<MacroStep>,
        getGesturePoints: suspend (gestureId: String) -> Pair<List<PointF>, Long>?
    ) {
        scope.launch {
            mutex.withLock {
                if (_engineState.value is MacroEngineState.Running) {
                    return@withLock
                }

                val activeSteps = steps.filter { it.isEnabled }.sortedBy { it.orderIndex }
                if (activeSteps.isEmpty()) {
                    _engineState.value = MacroEngineState.Error("هیچ مرحله فعالی در این ماکرو وجود ندارد.")
                    return@withLock
                }

                val generation = currentGeneration.incrementAndGet()
                _engineState.value = MacroEngineState.Running(
                    macroName = macro.name,
                    currentCycle = 1,
                    totalCycles = macro.repeatCount,
                    currentStepIndex = 0
                )

                currentExecutionJob = launch {
                    executeLoop(generation, macro, activeSteps, getGesturePoints)
                }
            }
        }
    }

    private suspend fun executeLoop(
        generation: Long,
        macro: Macro,
        steps: List<MacroStep>,
        getGesturePoints: suspend (gestureId: String) -> Pair<List<PointF>, Long>?
    ) {
        var cycle = 1
        val startTime = SystemClock.elapsedRealtime()

        try {
            while (isActive && currentGeneration.get() == generation) {
                if (macro.repeatCount in 1 until cycle) {
                    break
                }

                _engineState.value = MacroEngineState.Running(
                    macroName = macro.name,
                    currentCycle = cycle,
                    totalCycles = macro.repeatCount,
                    currentStepIndex = 0
                )

                for ((index, step) in steps.withIndex()) {
                    if (!isActive || currentGeneration.get() != generation) {
                        return
                    }

                    _engineState.value = MacroEngineState.Running(
                        macroName = macro.name,
                        currentCycle = cycle,
                        totalCycles = macro.repeatCount,
                        currentStepIndex = index + 1
                    )

                    // Execute step
                    val stepSuccess = executeStep(step, getGesturePoints)
                    if (!stepSuccess && step.stepType != StepType.WAIT) {
                        _engineState.value = MacroEngineState.Error("خطا در ارسال ژست مرحله ${index + 1}. دسترسی‌پذیری بررسی شود.")
                        return
                    }

                    // Step delay
                    if (step.delayMs > 0) {
                        delay(step.delayMs)
                    }
                }

                // Cycle interval with drift compensation
                if (macro.intervalMs > 0) {
                    val targetNextTime = startTime + (cycle * macro.intervalMs)
                    val remainingDelay = targetNextTime - SystemClock.elapsedRealtime()
                    if (remainingDelay > 0) {
                        delay(remainingDelay)
                    }
                }

                cycle++
            }

            _engineState.value = MacroEngineState.Idle
        } catch (e: CancellationException) {
            _engineState.value = MacroEngineState.Idle
        } catch (e: Exception) {
            _engineState.value = MacroEngineState.Error("خطای پیش‌بینی‌نشده در موتور: ${e.message}")
        }
    }

    private suspend fun executeStep(
        step: MacroStep,
        getGesturePoints: suspend (gestureId: String) -> Pair<List<PointF>, Long>?
    ): Boolean {
        return when (step.stepType) {
            StepType.WAIT -> {
                val waitTime = step.waitDurationMs ?: step.delayMs
                if (waitTime > 0) delay(waitTime)
                true
            }
            StepType.TAP -> {
                val gestureId = step.gestureId ?: return false
                val gestureData = getGesturePoints(gestureId) ?: return false
                val points = gestureData.first
                if (points.isEmpty()) return false
                val targetPoint = points.first()
                backend.dispatchTap(targetPoint.x, targetPoint.y, gestureData.second)
            }
            StepType.SWIPE -> {
                val gestureId = step.gestureId ?: return false
                val gestureData = getGesturePoints(gestureId) ?: return false
                val points = gestureData.first
                if (points.size < 2) return false
                backend.dispatchSwipe(points, gestureData.second)
            }
            StepType.HOLD -> {
                val gestureId = step.gestureId ?: return false
                val gestureData = getGesturePoints(gestureId) ?: return false
                val points = gestureData.first
                if (points.isEmpty()) return false
                val targetPoint = points.first()
                val success = backend.dispatchHoldSegment(targetPoint.x, targetPoint.y, isInitial = true, willContinue = false)
                backend.releaseHold(targetPoint.x, targetPoint.y)
                success
            }
        }
    }

    fun stop() {
        scope.launch {
            mutex.withLock {
                currentGeneration.incrementAndGet()
                currentExecutionJob?.cancel()
                currentExecutionJob = null
                backend.stopAll()
                _engineState.value = MacroEngineState.Stopping
                delay(80)
                _engineState.value = MacroEngineState.Idle
            }
        }
    }

    companion object {
        fun parsePointsJson(jsonStr: String): List<PointF> {
            val list = mutableListOf<PointF>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val pt = array.getJSONArray(i)
                    list.add(PointF(pt.getDouble(0).toFloat(), pt.getDouble(1).toFloat()))
                }
            } catch (e: Exception) {
                // Fallback default center point if parse fails
                list.add(PointF(540f, 1100f))
            }
            return list
        }
    }
}
