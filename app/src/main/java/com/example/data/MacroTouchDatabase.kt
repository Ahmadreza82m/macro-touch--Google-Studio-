package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        Profile::class,
        FloatingButton::class,
        Macro::class,
        GestureDef::class,
        MacroStep::class,
        ActivityLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MacroTouchDatabase : RoomDatabase() {

    abstract fun dao(): MacroTouchDao

    companion object {
        @Volatile
        private var INSTANCE: MacroTouchDatabase? = null

        fun getInstance(context: Context): MacroTouchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MacroTouchDatabase::class.java,
                    "macro_touch_pro.db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default profiles with neutral, honest template definitions (Section 7.2)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDefaultTemplates(getInstance(context))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultTemplates(db: MacroTouchDatabase) {
            val dao = db.dao()
            val defaultProfileId = UUID.randomUUID().toString()

            val defaultProfile = Profile(
                id = defaultProfileId,
                name = "پروفایل اصلی (کاربری عمومی)",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            dao.insertProfile(defaultProfile)

            // Seed sample neutral template: Auto Tap (با برچسب قالب؛ مختصات باید توسط کاربر تنظیم شود)
            val autoTapMacroId = UUID.randomUUID().toString()
            val autoTapMacro = Macro(
                id = autoTapMacroId,
                profileId = defaultProfileId,
                name = "تپ خودکار (قالب؛ مطابق صفحه خودتان تنظیم کنید)",
                repeatCount = 10,
                intervalMs = 250L,
                isEnabled = true
            )
            dao.insertMacro(autoTapMacro)

            // Gesture: Center tap placeholder
            val tapGestureId = UUID.randomUUID().toString()
            val tapGesture = GestureDef(
                id = tapGestureId,
                profileId = defaultProfileId,
                name = "نقطه لمس مرکزی",
                pointsJson = "[[540.0, 1100.0]]",
                durationMs = 50L
            )
            dao.insertGesture(tapGesture)

            val tapStep = MacroStep(
                id = UUID.randomUUID().toString(),
                macroId = autoTapMacroId,
                orderIndex = 1,
                stepType = StepType.TAP,
                isEnabled = true,
                delayMs = 200L,
                gestureId = tapGestureId
            )
            dao.insertStep(tapStep)

            // Seed floating button
            val mainButton = FloatingButton(
                id = UUID.randomUUID().toString(),
                profileId = defaultProfileId,
                label = "T1",
                x = 60f,
                y = 400f,
                sizeDp = 56,
                alpha = 0.95f,
                colorHex = "#F2A93B",
                shortTapMacroId = autoTapMacroId,
                shortTapEnabled = true,
                doubleTapMacroId = null,
                doubleTapEnabled = false,
                longPressMacroId = null,
                longPressEnabled = false,
                releaseMacroId = null,
                releaseEnabled = false
            )
            dao.insertButton(mainButton)

            dao.insertLog(
                ActivityLog(
                    type = "PROFILE_CHANGE",
                    message = "پایگاه داده راه‌اندازی و پروفایل اولیه بارگذاری شد."
                )
            )
        }
    }
}
