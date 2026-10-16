package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MacroEngineState
import com.example.ui.UiState
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    state: UiState,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ColorVoid)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // App Header with signature
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Macro Touch Pro",
                        color = ColorGold300,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Crafted by Ahmadreza © 2026",
                        color = ColorTextMuted,
                        fontSize = 12.sp
                    )
                }

                // Active Profile Pill
                Surface(
                    onClick = onNavigateToProfiles,
                    shape = RoundedCornerShape(20.dp),
                    color = ColorSurface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorGoldBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ColorEmerald)
                        )
                        Text(
                            text = state.activeProfile?.name ?: "پروفایل پیش‌فرض",
                            color = ColorTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "انتخاب پروفایل",
                            tint = ColorGold300,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Engine Status & Quick Controls
        item {
            EngineStatusCard(
                engineState = state.engineState,
                onStart = onStartEngine,
                onStop = onStopEngine
            )
        }

        // System Health & Permissions Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سلامت و دسترسی‌های سیستم",
                    color = ColorTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PermissionStatusCard(
                        title = "دسترسی‌پذیری لمس",
                        subtitle = "dispatchGesture",
                        isGranted = state.isAccessibilityGranted,
                        onClick = onNavigateToOnboarding,
                        modifier = Modifier.weight(1f)
                    )

                    PermissionStatusCard(
                        title = "پنجره‌های شناور",
                        subtitle = "Overlay Window",
                        isGranted = state.isOverlayGranted,
                        onClick = onNavigateToOnboarding,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Honest Disclosure Notice Banner (Section 1 & 7.1)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ColorSurface2,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x333FA7FF))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "اطلاعیه فنی",
                        tint = ColorInfo,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "شفافیت فنی و عملکرد واقعی",
                            color = ColorInfo,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اجرای ژست با dispatchGesture انجام می‌شود که تأخیر ذاتی وابسته به سخت‌افزار دستگاه دارد. دکمه توقف (Stop) چرخه‌های بعدی را متوقف می‌کند اما ژست‌های در حال اجرای اندروید تا انتها طی می‌شوند.",
                            color = ColorTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Recent Activity History (Section 6 & 7.1)
        item {
            Text(
                text = "سابقه فعالیت‌های واقعی",
                color = ColorTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (state.activityLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنوز رویدادی ثبت نشده است.",
                        color = ColorTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(state.activityLogs.take(5)) { log ->
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ColorSurface1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.message,
                            color = ColorTextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = timeStr,
                            color = ColorTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EngineStatusCard(
    engineState: MacroEngineState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isRunning = engineState is MacroEngineState.Running

    // Pulse animation for running state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ColorSurface1,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isRunning) ColorGold500 else ColorGoldBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (engineState) {
                                    is MacroEngineState.Running -> ColorGold500.copy(alpha = pulseAlpha)
                                    is MacroEngineState.Stopping -> ColorDanger
                                    is MacroEngineState.Error -> ColorDanger
                                    else -> ColorTextMuted
                                }
                            )
                    )

                    Text(
                        text = when (engineState) {
                            is MacroEngineState.Idle -> "آماده به‌کار (Idle)"
                            is MacroEngineState.Running -> "در حال اجرا (Running)"
                            is MacroEngineState.Paused -> "مکث (Paused)"
                            is MacroEngineState.Stopping -> "در حال توقف..."
                            is MacroEngineState.Error -> "خطای اجرا"
                        },
                        color = ColorTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isRunning) {
                    val runState = engineState as MacroEngineState.Running
                    Text(
                        text = "چرخه ${runState.currentCycle} | مرحله ${runState.currentStepIndex}",
                        color = ColorGold300,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Execution Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStart,
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorGold500,
                        contentColor = ColorVoid,
                        disabledContainerColor = ColorSurface3,
                        disabledContentColor = ColorTextMuted
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_start_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "شروع ماکرو",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("شروع ماکرو", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onStop,
                    enabled = isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorDanger,
                        contentColor = ColorTextPrimary,
                        disabledContainerColor = ColorSurface3,
                        disabledContentColor = ColorTextMuted
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "توقف ماکرو",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("توقف (Stop)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = ColorSurface1,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) Color(0x3334D17E) else Color(0x33FF5A4E)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = ColorTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) ColorEmerald else ColorDanger)
                )
            }

            Text(
                text = if (isGranted) "فعال و آماده" else "نیاز به فعال‌سازی",
                color = if (isGranted) ColorEmerald else ColorDanger,
                fontSize = 11.sp
            )
            Text(
                text = subtitle,
                color = ColorTextMuted,
                fontSize = 10.sp
            )
        }
    }
}
