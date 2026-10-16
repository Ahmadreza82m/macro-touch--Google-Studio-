package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Profile
import com.example.ui.UiState
import com.example.ui.theme.*

@Composable
fun ProfilesScreen(
    state: UiState,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onRestoreProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var showRecycleBin by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ColorVoid)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مدیریت پروفایل‌ها",
                        color = ColorTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "جداسازی کامل دکمه‌ها و ماکروها",
                        color = ColorTextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showRecycleBin = !showRecycleBin },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showRecycleBin) ColorGold900 else ColorSurface2,
                            contentColor = ColorGold300
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "سطل بازیافت"
                        )
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorGold500,
                            contentColor = ColorVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("create_profile_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جدید", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showRecycleBin) {
            // Recycle bin view
            item {
                Text(
                    text = "سطل بازیافت (پروفایل‌های حذف شده)",
                    color = ColorDanger,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.recycleBinProfiles.isEmpty()) {
                item {
                    Text(
                        text = "سطل بازیافت خالی است.",
                        color = ColorTextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                items(state.recycleBinProfiles) { profile ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ColorSurface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.name,
                                color = ColorTextMuted,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { onRestoreProfile(profile.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ColorSurface3,
                                    contentColor = ColorGold300
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("بازگردانی", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Active profiles list
            item {
                Text(
                    text = "پروفایل‌های فعال",
                    color = ColorTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(state.profiles) { profile ->
                val isActive = profile.id == state.activeProfile?.id
                Surface(
                    onClick = { onSelectProfile(profile.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActive) ColorSurface2 else ColorSurface1,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isActive) ColorGold500 else ColorSurface3
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = profile.name,
                                    color = if (isActive) ColorGold300 else ColorTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ColorGold900
                                    ) {
                                        Text(
                                            text = "فعال",
                                            color = ColorGold100,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "شناسه پایدار: ${profile.id.take(8)}...",
                                color = ColorTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { onDeleteProfile(profile.id) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = ColorTextMuted)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف به سطل زباله")
                        }
                    }
                }
            }

            // Built-in neutral templates section (Section 7.2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "قالب‌های آماده قابل ویرایش",
                        color = ColorTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "مختصات این قالب‌ها نگهدارنده جایگاه هستند و باید در اولین اجرا متناسب با صفحه شما کالیبره شوند.",
                        color = ColorTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            item {
                TemplateCard(
                    title = "Auto Tap (تپ خودکار)",
                    tag = "قالب؛ نیاز به تنظیم مختصات",
                    description = "تکرار ضربه در یک موقعیت مشخص با بازه زمانی قابل کنترل.",
                    onAdd = { onCreateProfile("پروفایل تپ خودکار") }
                )
            }

            item {
                TemplateCard(
                    title = "Double-Tap Pattern (الگوی دوضربه)",
                    tag = "قالب؛ نیاز به تنظیم مختصات",
                    description = "ارسال دو لمس پیاپی با فاصله تأخیر معین.",
                    onAdd = { onCreateProfile("پروفایل الگوی دوضربه") }
                )
            }

            item {
                TemplateCard(
                    title = "Hold & Release (نگه‌داشتن و رهاسازی)",
                    tag = "قالب؛ نیاز به تنظیم مختصات",
                    description = "لمس پیوسته با قطعه‌های continueStroke و رها کردن در انتها.",
                    onAdd = { onCreateProfile("پروفایل نگه‌داشتن و رهاسازی") }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("ساخت پروفایل جدید", color = ColorTextPrimary) },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("نام پروفایل") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorGold500,
                        unfocusedBorderColor = ColorSurface3,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            onCreateProfile(newProfileName.trim())
                            newProfileName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorGold500, contentColor = ColorVoid)
                ) {
                    Text("ایجاد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("انصراف", color = ColorTextSecondary)
                }
            },
            containerColor = ColorSurface2
        )
    }
}

@Composable
fun TemplateCard(
    title: String,
    tag: String,
    description: String,
    onAdd: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ColorSurface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorSurface3),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = ColorGold300,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ColorSurface3
                ) {
                    Text(
                        text = tag,
                        color = ColorTextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = description,
                color = ColorTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorSurface2,
                    contentColor = ColorGold300
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("استفاده از این قالب", fontSize = 11.sp)
            }
        }
    }
}
