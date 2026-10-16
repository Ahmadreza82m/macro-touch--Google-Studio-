package com.example.ui.screens

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TouchDetectorConfig
import com.example.model.ButtonBehaviorType
import com.example.ui.UiState
import com.example.ui.theme.*

@Composable
fun GestureAndSettingsScreen(
    state: UiState,
    onUpdateConfig: (TouchDetectorConfig) -> Unit,
    onSimulateTouch: (ButtonBehaviorType) -> Unit,
    modifier: Modifier = Modifier
) {
    var drawnPoints by remember { mutableStateOf(listOf(PointF(200f, 150f), PointF(400f, 350f))) }
    var currentConfig by remember { mutableStateOf(state.touchConfig) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ColorVoid)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "تنظیمات عددی و پد آزمایش لمس",
                    color = ColorTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "آستانه‌های زمانی دقیق و آزمایش رفتارها",
                    color = ColorTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Interactive Touch Test Pad (Section 7.7)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ColorSurface1,
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorGoldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "پد آزمایش لمس (Touch Test Pad)",
                            color = ColorGold300,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (state.lastDetectedTouchBehavior != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ColorGold900
                            ) {
                                Text(
                                    text = when (state.lastDetectedTouchBehavior) {
                                        ButtonBehaviorType.SHORT_TAP -> "تپ کوتاه"
                                        ButtonBehaviorType.DOUBLE_TAP -> "دو تپ (Double Tap)"
                                        ButtonBehaviorType.LONG_PRESS -> "لمس طولانی (Long Press)"
                                        ButtonBehaviorType.RELEASE -> "رها کردن (Release)"
                                    },
                                    color = ColorGold100,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Test surface
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorSurface2)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onSimulateTouch(ButtonBehaviorType.SHORT_TAP) },
                                    onDoubleTap = { onSimulateTouch(ButtonBehaviorType.DOUBLE_TAP) },
                                    onLongPress = { onSimulateTouch(ButtonBehaviorType.LONG_PRESS) },
                                    onPress = {
                                        tryAwaitRelease()
                                        onSimulateTouch(ButtonBehaviorType.RELEASE)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "در این قسمت تپ، دوبار ضربه بزنید یا نگه‌دارید تا رفتار لمس تشخیص داده شود.",
                            color = ColorTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // Numeric Threshold Controls (Section 7.7)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ColorSurface1,
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorSurface3),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "آستانه‌های زمانی موتور تشخیص لمس",
                        color = ColorTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Long press threshold
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("آستانه لمس طولانی (Long Press)", color = ColorTextSecondary, fontSize = 12.sp)
                            Text("${currentConfig.longPressThresholdMs}ms", color = ColorGold300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = currentConfig.longPressThresholdMs.toFloat(),
                            onValueChange = {
                                currentConfig = currentConfig.copy(longPressThresholdMs = it.toLong())
                                onUpdateConfig(currentConfig)
                            },
                            valueRange = 200f..2000f,
                            steps = 17,
                            colors = SliderDefaults.colors(
                                thumbColor = ColorGold500,
                                activeTrackColor = ColorGold500,
                                inactiveTrackColor = ColorSurface3
                            )
                        )
                    }

                    // Double tap interval
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("بازه مجاز دو لمس (Double Tap Window)", color = ColorTextSecondary, fontSize = 12.sp)
                            Text("${currentConfig.doubleTapIntervalMs}ms", color = ColorGold300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = currentConfig.doubleTapIntervalMs.toFloat(),
                            onValueChange = {
                                currentConfig = currentConfig.copy(doubleTapIntervalMs = it.toLong())
                                onUpdateConfig(currentConfig)
                            },
                            valueRange = 100f..600f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = ColorGold500,
                                activeTrackColor = ColorGold500,
                                inactiveTrackColor = ColorSurface3
                            )
                        )
                    }

                    // Honest latency explanation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorSurface2)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ColorInfo, modifier = Modifier.size(16.dp))
                        Text(
                            text = "نکته فنی: با فعال بودن دو‌لمس، تپ کوتاه دقیقاً به میزان بازه دو‌لمس دیرتر اجرا می‌شود تا سیستم مطمئن شود ضربه دومی در کار نیست.",
                            color = ColorTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Gesture Visual Canvas Preview (Section 7.6)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ColorSurface1,
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorSurface3),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ویرایشگر ژست بصری (Visual Polyline Canvas)",
                        color = ColorTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "برای تعریف مسیر حرکت روی کادر ضربه بزنید.",
                        color = ColorTextMuted,
                        fontSize = 11.sp
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ColorSurface2)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    drawnPoints = drawnPoints + PointF(offset.x, offset.y)
                                }
                            }
                    ) {
                        for (i in 0 until drawnPoints.size - 1) {
                            val p1 = drawnPoints[i]
                            val p2 = drawnPoints[i + 1]
                            drawLine(
                                color = ColorGold500,
                                start = Offset(p1.x, p1.y),
                                end = Offset(p2.x, p2.y),
                                strokeWidth = 4f
                            )
                        }

                        for (pt in drawnPoints) {
                            drawCircle(
                                color = ColorGold300,
                                radius = 7f,
                                center = Offset(pt.x, pt.y)
                            )
                        }
                    }

                    Text(
                        text = "تعداد نقاط مسیر: ${drawnPoints.size} نقطه",
                        color = ColorTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
