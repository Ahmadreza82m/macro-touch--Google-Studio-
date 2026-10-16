package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Macro
import com.example.model.MacroStep
import com.example.model.StepType
import com.example.ui.UiState
import com.example.ui.theme.*

@Composable
fun MacroEditorScreen(
    state: UiState,
    onSelectMacro: (Macro) -> Unit,
    onUpdateMacro: (Macro) -> Unit,
    onToggleStep: (MacroStep) -> Unit,
    onAddStep: (StepType) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMacro = state.selectedMacro
    val steps = state.stepsForSelectedMacro

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ColorVoid)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Macro Header & Selection
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ویرایشگر مراحل ماکرو",
                    color = ColorTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تایم‌لاین زمانی و گام‌های اجرایی مستقل",
                    color = ColorTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Loop / Interval Configuration Card
        if (selectedMacro != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ColorSurface1,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorGoldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تنظیمات تکرار: ${selectedMacro.name}",
                            color = ColorGold300,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تعداد تکرار", color = ColorTextSecondary, fontSize = 11.sp)
                                Text(
                                    text = if (selectedMacro.repeatCount == 0) "بی‌پایان (تا زمان فشردن Stop)" else "${selectedMacro.repeatCount} بار",
                                    color = ColorTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("فاصله بین چرخه‌ها", color = ColorTextSecondary, fontSize = 11.sp)
                                Text(
                                    text = "${selectedMacro.intervalMs} میلی‌ثانیه",
                                    color = ColorTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Timeline Steps Header & Add Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تایم‌لاین گام‌ها (${steps.size})",
                    color = ColorTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onAddStep(StepType.TAP) },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSurface2, contentColor = ColorGold300),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+ تپ", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onAddStep(StepType.SWIPE) },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSurface2, contentColor = ColorInfo),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+ کشیدن", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onAddStep(StepType.WAIT) },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSurface2, contentColor = ColorEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+ انتظار", fontSize = 11.sp)
                    }
                }
            }
        }

        // Timeline Step Items
        if (steps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هیچ مرحله‌ای برای این ماکرو تعریف نشده است. از دکمه‌های بالا گام اضافه کنید.",
                        color = ColorTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            itemsIndexed(steps) { index, step ->
                StepTimelineCard(
                    stepNumber = index + 1,
                    step = step,
                    onToggle = { onToggleStep(step) }
                )
            }
        }
    }
}

@Composable
fun StepTimelineCard(
    stepNumber: Int,
    step: MacroStep,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ColorSurface1,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (step.isEnabled) ColorSurface3 else Color(0x22FFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Number badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (step.stepType) {
                                StepType.TAP -> ColorGold900
                                StepType.SWIPE -> Color(0x333FA7FF)
                                StepType.HOLD -> Color(0x33A56BFF)
                                StepType.WAIT -> Color(0x3334D17E)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepNumber",
                        color = when (step.stepType) {
                            StepType.TAP -> ColorGold300
                            StepType.SWIPE -> ColorInfo
                            StepType.HOLD -> ColorViolet
                            StepType.WAIT -> ColorEmerald
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when (step.stepType) {
                            StepType.TAP -> "ضربه (Tap)"
                            StepType.SWIPE -> "کشیدن (Swipe)"
                            StepType.HOLD -> "نگه‌داشتن (Hold)"
                            StepType.WAIT -> "تأخیر زمانی (Wait)"
                        },
                        color = if (step.isEnabled) ColorTextPrimary else ColorTextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "تأخیر بعد از اجرا: ${step.delayMs}ms",
                        color = ColorTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = step.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorGold500,
                    checkedTrackColor = ColorGold900,
                    uncheckedThumbColor = ColorTextMuted,
                    uncheckedTrackColor = ColorSurface2
                )
            )
        }
    }
}
