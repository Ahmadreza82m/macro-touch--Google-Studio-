package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MacroTouchDatabase
import com.example.data.MacroTouchRepository
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val activeProfile: Profile? = null,
    val profiles: List<Profile> = emptyList(),
    val recycleBinProfiles: List<Profile> = emptyList(),
    val macros: List<Macro> = emptyList(),
    val selectedMacro: Macro? = null,
    val stepsForSelectedMacro: List<MacroStep> = emptyList(),
    val gestures: List<GestureDef> = emptyList(),
    val floatingButtons: List<FloatingButton> = emptyList(),
    val activityLogs: List<ActivityLog> = emptyList(),
    val engineState: MacroEngineState = MacroEngineState.Idle,
    val isAccessibilityGranted: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val touchConfig: TouchDetectorConfig = TouchDetectorConfig(),
    val lastDetectedTouchBehavior: ButtonBehaviorType? = null,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MacroTouchRepository
    val engine: MacroEngine

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val db = MacroTouchDatabase.getInstance(application)
        repository = MacroTouchRepository(db.dao())
        engine = MacroEngine(MockBackend()) // Defaults to MockBackend for safety / preview

        // Observe active profile
        viewModelScope.launch {
            repository.activeProfile.collect { profile ->
                _uiState.update { it.copy(activeProfile = profile) }
                if (profile != null) {
                    loadDataForProfile(profile.id)
                }
            }
        }

        // Observe active profiles list
        viewModelScope.launch {
            repository.activeProfiles.collect { list ->
                _uiState.update { it.copy(profiles = list) }
            }
        }

        // Observe recycle bin
        viewModelScope.launch {
            repository.recycleBinProfiles.collect { list ->
                _uiState.update { it.copy(recycleBinProfiles = list) }
            }
        }

        // Observe logs
        viewModelScope.launch {
            repository.recentActivityLogs.collect { logs ->
                _uiState.update { it.copy(activityLogs = logs) }
            }
        }

        // Observe engine state
        viewModelScope.launch {
            engine.engineState.collect { state ->
                _uiState.update { it.copy(engineState = state) }
            }
        }
    }

    private fun loadDataForProfile(profileId: String) {
        viewModelScope.launch {
            repository.getMacros(profileId).collect { macros ->
                _uiState.update { it.copy(macros = macros) }
                if (_uiState.value.selectedMacro == null && macros.isNotEmpty()) {
                    selectMacro(macros.first())
                }
            }
        }

        viewModelScope.launch {
            repository.getGestures(profileId).collect { gestures ->
                _uiState.update { it.copy(gestures = gestures) }
            }
        }

        viewModelScope.launch {
            repository.getButtons(profileId).collect { buttons ->
                _uiState.update { it.copy(floatingButtons = buttons) }
            }
        }
    }

    fun selectMacro(macro: Macro) {
        _uiState.update { it.copy(selectedMacro = macro) }
        viewModelScope.launch {
            repository.getSteps(macro.id).collect { steps ->
                _uiState.update { it.copy(stepsForSelectedMacro = steps) }
            }
        }
    }

    fun startCurrentMacro() {
        val macro = _uiState.value.selectedMacro ?: return
        val steps = _uiState.value.stepsForSelectedMacro
        if (steps.isEmpty()) return

        engine.startMacro(macro, steps) { gestureId ->
            val gesture = repository.getGesture(gestureId) ?: return@startMacro null
            val points = MacroEngine.parsePointsJson(gesture.pointsJson)
            Pair(points, gesture.durationMs)
        }

        viewModelScope.launch {
            repository.logActivity("EXECUTION", "ماکرو «${macro.name}» شروع شد.")
        }
    }

    fun stopEngine() {
        engine.stop()
        viewModelScope.launch {
            repository.logActivity("EXECUTION", "توقف ماکرو درخواست شد.")
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            repository.switchProfile(profileId)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            val p = repository.createProfile(name)
            repository.switchProfile(p.id)
        }
    }

    fun softDeleteProfile(profileId: String) {
        viewModelScope.launch {
            repository.softDeleteProfile(profileId)
        }
    }

    fun restoreProfile(profileId: String) {
        viewModelScope.launch {
            repository.restoreProfile(profileId)
        }
    }

    fun updateTouchConfig(config: TouchDetectorConfig) {
        _uiState.update { it.copy(touchConfig = config) }
    }

    fun onTestPadBehaviorDetected(type: ButtonBehaviorType) {
        _uiState.update { it.copy(lastDetectedTouchBehavior = type) }
    }

    fun setPermissionsState(accessibility: Boolean, overlay: Boolean) {
        _uiState.update {
            it.copy(
                isAccessibilityGranted = accessibility,
                isOverlayGranted = overlay
            )
        }
    }
}
