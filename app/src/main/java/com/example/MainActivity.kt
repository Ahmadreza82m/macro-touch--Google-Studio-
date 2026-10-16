package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.StepType
import com.example.service.TouchAutomationService
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class AppScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("داشبورد", Icons.Default.Dashboard),
    PROFILES("پروفایل‌ها", Icons.Default.AccountTree),
    MACRO_EDITOR("ویرایشگر", Icons.Default.Timeline),
    SETTINGS("تنظیمات و پد", Icons.Default.Tune),
    ONBOARDING("مجوزها", Icons.Default.Security)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MacroTouchTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // BackHandler to return to Dashboard from secondary screens
                BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
                    currentScreen = AppScreen.DASHBOARD
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentScreen != AppScreen.ONBOARDING) {
                            NavigationBar(
                                containerColor = ColorSurface1,
                                contentColor = ColorGold300,
                                tonalElevation = 8.dp
                            ) {
                                val navItems = listOf(
                                    AppScreen.DASHBOARD,
                                    AppScreen.PROFILES,
                                    AppScreen.MACRO_EDITOR,
                                    AppScreen.SETTINGS
                                )
                                for (screen in navItems) {
                                    val isSelected = currentScreen == screen
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title
                                            )
                                        },
                                        label = { Text(screen.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ColorVoid,
                                            selectedTextColor = ColorGold300,
                                            indicatorColor = ColorGold500,
                                            unselectedIconColor = ColorTextMuted,
                                            unselectedTextColor = ColorTextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorVoid)
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> DashboardScreen(
                                state = uiState,
                                onStartEngine = { viewModel.startCurrentMacro() },
                                onStopEngine = { viewModel.stopEngine() },
                                onNavigateToProfiles = { currentScreen = AppScreen.PROFILES },
                                onNavigateToOnboarding = { currentScreen = AppScreen.ONBOARDING }
                            )

                            AppScreen.PROFILES -> ProfilesScreen(
                                state = uiState,
                                onSelectProfile = { viewModel.switchProfile(it) },
                                onCreateProfile = { viewModel.createProfile(it) },
                                onDeleteProfile = { viewModel.softDeleteProfile(it) },
                                onRestoreProfile = { viewModel.restoreProfile(it) }
                            )

                            AppScreen.MACRO_EDITOR -> MacroEditorScreen(
                                state = uiState,
                                onSelectMacro = { viewModel.selectMacro(it) },
                                onUpdateMacro = {},
                                onToggleStep = {},
                                onAddStep = { stepType -> }
                            )

                            AppScreen.SETTINGS -> GestureAndSettingsScreen(
                                state = uiState,
                                onUpdateConfig = { viewModel.updateTouchConfig(it) },
                                onSimulateTouch = { viewModel.onTestPadBehaviorDetected(it) }
                            )

                            AppScreen.ONBOARDING -> OnboardingScreen(
                                state = uiState,
                                onBack = { currentScreen = AppScreen.DASHBOARD }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val accessibilityGranted = enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }

        viewModel.setPermissionsState(accessibilityGranted, overlayGranted)
    }
}
