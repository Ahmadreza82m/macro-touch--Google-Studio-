package com.example.data

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroTouchDao {

    // --- Profiles ---
    @Query("SELECT * FROM profiles WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllActiveProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getRecycleBinProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): Profile?

    @Query("SELECT * FROM profiles WHERE isActive = 1 AND deletedAt IS NULL LIMIT 1")
    fun getActiveProfile(): Flow<Profile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun clearActiveProfiles()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun setActiveProfile(id: String)

    @Query("UPDATE profiles SET deletedAt = :timestamp, isActive = 0 WHERE id = :id")
    suspend fun softDeleteProfile(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE profiles SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreProfile(id: String)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun permanentlyDeleteProfile(id: String)

    // --- Floating Buttons ---
    @Query("SELECT * FROM floating_buttons WHERE profileId = :profileId")
    fun getButtonsForProfile(profileId: String): Flow<List<FloatingButton>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButton(button: FloatingButton)

    @Update
    suspend fun updateButton(button: FloatingButton)

    @Delete
    suspend fun deleteButton(button: FloatingButton)

    // --- Macros ---
    @Query("SELECT * FROM macros WHERE profileId = :profileId AND deletedAt IS NULL")
    fun getMacrosForProfile(profileId: String): Flow<List<Macro>>

    @Query("SELECT * FROM macros WHERE id = :id LIMIT 1")
    suspend fun getMacroById(id: String): Macro?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: Macro)

    @Update
    suspend fun updateMacro(macro: Macro)

    @Query("UPDATE macros SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteMacro(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE macros SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreMacro(id: String)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun permanentlyDeleteMacro(id: String)

    // --- Gestures ---
    @Query("SELECT * FROM gesture_defs WHERE profileId = :profileId")
    fun getGesturesForProfile(profileId: String): Flow<List<GestureDef>>

    @Query("SELECT * FROM gesture_defs WHERE id = :id LIMIT 1")
    suspend fun getGestureById(id: String): GestureDef?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGesture(gesture: GestureDef)

    @Delete
    suspend fun deleteGesture(gesture: GestureDef)

    // --- Steps ---
    @Query("SELECT * FROM macro_steps WHERE macroId = :macroId ORDER BY orderIndex ASC")
    fun getStepsForMacro(macroId: String): Flow<List<MacroStep>>

    @Query("SELECT * FROM macro_steps WHERE macroId = :macroId ORDER BY orderIndex ASC")
    suspend fun getStepsForMacroList(macroId: String): List<MacroStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: MacroStep)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<MacroStep>)

    @Update
    suspend fun updateStep(step: MacroStep)

    @Delete
    suspend fun deleteStep(step: MacroStep)

    @Query("DELETE FROM macro_steps WHERE macroId = :macroId")
    suspend fun deleteStepsForMacro(macroId: String)

    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
