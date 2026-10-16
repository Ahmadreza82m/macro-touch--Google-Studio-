package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UiState
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
    state: UiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ColorVoid)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = ColorTextPrimary)
                }
                Column {
                    Text(
                        text = "راهنمای فعال‌سازی و مجوزها",
                        color = ColorTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "بدون مجوز اینترنت؛ تمام پردازش‌ها کاملاً محلی روی گوشی انجام می‌شود",
                        color = ColorEmerald,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Step 1: Overlay Window
        item {
            OnboardingStepCard(
                step = 1,
                title = "۱. مجوز پنجره شناور (Overlay Window)",
                description = "برای ترسیم دکمه‌های کنترلی روی بازی‌ها و اپلیکیشن‌ها نیاز است.",
                actionLabel = "باز کردن تنظیمات پنجره شناور",
                isCompleted = state.isOverlayGranted,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }

        // Step 2: Accessibility Service
        item {
            OnboardingStepCard(
                step = 2,
                title = "۲. سرویس دسترسی‌پذیری (Accessibility Service)",
                description = "برای ارسال ضربه، کشیدن و نگه‌داشتن با dispatchGesture. برنامه هیچ متنی از صفحه نمی‌خواند.",
                actionLabel = "باز کردن تنظیمات دسترسی‌پذیری",
                isCompleted = state.isAccessibilityGranted,
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }

        // Step 3: Restricted Settings Android 13+ (Section 7.8)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ColorSurface1,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFC857)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ColorGold300, modifier = Modifier.size(20.dp))
                        Text(
                            text = "نکته مهم اندروید ۱۳ به بالا (Restricted Settings)",
                            color = ColorGold300,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "در اندروید ۱۳+ برای برنامه‌هایی که مستقیماً (APK) نصب می‌شوند، گزینه دسترسی‌پذیری خاکستری و غیرفعال است. راه حل:",
                        color = ColorTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "۱. وارد تنظیمات گوشی > برنامه‌ها > Macro Touch Pro شوید.\n" +
                               "۲. روی علامت سه نقطه در گوشه بالا سمت راست ضربه بزنید.\n" +
                               "۳. گزینه «اجازه دادن به تنظیمات محدود» (Allow restricted settings) را بزنید.\n" +
                               "۴. اکنون سرویس دسترسی‌پذیری فعال می‌شود.",
                        color = ColorTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorSurface2,
                            contentColor = ColorGold300
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("رفتن به صفحه اطلاعات برنامه (App Info)", fontSize = 11.sp)
                    }
                }
            }
        }

        // Step 4: Battery Optimization Guidance for Manufacturers (Section 7.8)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ColorSurface1,
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorSurface3),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = ColorEmerald, modifier = Modifier.size(20.dp))
                        Text(
                            text = "جلوگیری از بسته‌شدن خودکار سرویس (باتری)",
                            color = ColorEmerald,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "رابط‌های کاربری مانند Xiaomi (MIUI/HyperOS)، Samsung (One UI) و Huawei سرویس‌های پس‌زمینه را متوقف می‌کنند:",
                        color = ColorTextSecondary,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "• شیائومی: به بخش App Info رفته، Autostart را روشن و Battery Saver را روی No Restrictions بگذارید.\n" +
                               "• سامسونگ: برنامه را در لیست Never Sleeping Apps قرار دهید.\n" +
                               "• سایر دستگاه‌ها: بهینه‌سازی باتری (Battery Optimization) را غیرفعال کنید.",
                        color = ColorTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingStepCard(
    step: Int,
    title: String,
    description: String,
    actionLabel: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ColorSurface1,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCompleted) Color(0x3334D17E) else ColorSurface3
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = ColorTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) ColorEmerald else ColorGold500)
                )
            }

            Text(
                text = description,
                color = ColorTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) ColorSurface2 else ColorGold500,
                    contentColor = if (isCompleted) ColorEmerald else ColorVoid
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCompleted) "مجوز داده شده است ✓" else actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
