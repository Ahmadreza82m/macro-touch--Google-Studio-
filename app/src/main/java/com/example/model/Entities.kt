package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

enum class ButtonBehaviorType {
    SHORT_TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    RELEASE
}

@Entity(
    tableName = "floating_buttons",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Macro::class,
            parentColumns = ["id"],
            childColumns = ["shortTapMacroId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Macro::class,
            parentColumns = ["id"],
            childColumns = ["doubleTapMacroId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Macro::class,
            parentColumns = ["id"],
            childColumns = ["longPressMacroId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Macro::class,
            parentColumns = ["id"],
            childColumns = ["releaseMacroId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("profileId"),
        Index("shortTapMacroId"),
        Index("doubleTapMacroId"),
        Index("longPressMacroId"),
        Index("releaseMacroId")
    ]
)
data class FloatingButton(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val label: String,
    val x: Float = 100f,
    val y: Float = 300f,
    val sizeDp: Int = 56,
    val alpha: Float = 0.9f,
    val colorHex: String = "#F2A93B",
    val shortTapMacroId: String? = null,
    val shortTapEnabled: Boolean = true,
    val doubleTapMacroId: String? = null,
    val doubleTapEnabled: Boolean = false,
    val longPressMacroId: String? = null,
    val longPressEnabled: Boolean = false,
    val releaseMacroId: String? = null,
    val releaseEnabled: Boolean = false
)

@Entity(
    tableName = "macros",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class Macro(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val name: String,
    val repeatCount: Int = 1, // 0 = infinite loop
    val intervalMs: Long = 100L,
    val isEnabled: Boolean = true,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "gesture_defs",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class GestureDef(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val name: String,
    val pointsJson: String, // Polyline points serialized as JSON [[x, y], ...]
    val durationMs: Long = 300L
)

enum class StepType {
    TAP,
    SWIPE,
    HOLD,
    WAIT
}

@Entity(
    tableName = "macro_steps",
    foreignKeys = [
        ForeignKey(
            entity = Macro::class,
            parentColumns = ["id"],
            childColumns = ["macroId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GestureDef::class,
            parentColumns = ["id"],
            childColumns = ["gestureId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("macroId"),
        Index("gestureId")
    ]
)
data class MacroStep(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val macroId: String,
    val orderIndex: Int,
    val stepType: StepType,
    val isEnabled: Boolean = true,
    val delayMs: Long = 50L,
    val gestureId: String? = null,
    val waitDurationMs: Long? = null // only valid when stepType == WAIT
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // EXECUTION, PROFILE_CHANGE, SETTINGS_CHANGE, IMPORT_BACKUP
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
