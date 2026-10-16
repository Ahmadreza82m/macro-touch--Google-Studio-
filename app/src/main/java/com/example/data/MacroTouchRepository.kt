package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.security.MessageDigest

class MacroTouchRepository(private val dao: MacroTouchDao) {

    val activeProfiles: Flow<List<Profile>> = dao.getAllActiveProfiles()
    val recycleBinProfiles: Flow<List<Profile>> = dao.getRecycleBinProfiles()
    val activeProfile: Flow<Profile?> = dao.getActiveProfile()
    val recentActivityLogs: Flow<List<ActivityLog>> = dao.getRecentActivityLogs()

    suspend fun createProfile(name: String): Profile {
        val profile = Profile(name = name)
        dao.insertProfile(profile)
        dao.insertLog(ActivityLog(type = "PROFILE_CHANGE", message = "پروفایل جدید ساخته شد: $name"))
        return profile
    }

    suspend fun switchProfile(profileId: String) {
        dao.clearActiveProfiles()
        dao.setActiveProfile(profileId)
        dao.insertLog(ActivityLog(type = "PROFILE_CHANGE", message = "پروفایل فعال تغییر یافت."))
    }

    suspend fun softDeleteProfile(profileId: String) {
        dao.softDeleteProfile(profileId)
        dao.insertLog(ActivityLog(type = "PROFILE_CHANGE", message = "پروفایل به سطل زباله منتقل شد."))
    }

    suspend fun restoreProfile(profileId: String) {
        dao.restoreProfile(profileId)
        dao.insertLog(ActivityLog(type = "PROFILE_CHANGE", message = "پروفایل از سطل بازیافت بازیابی شد."))
    }

    suspend fun permanentlyDeleteProfile(profileId: String) {
        dao.permanentlyDeleteProfile(profileId)
        dao.insertLog(ActivityLog(type = "PROFILE_CHANGE", message = "پروفایل برای همیشه حذف شد."))
    }

    fun getButtons(profileId: String): Flow<List<FloatingButton>> = dao.getButtonsForProfile(profileId)
    suspend fun saveButton(button: FloatingButton) = dao.insertButton(button)
    suspend fun deleteButton(button: FloatingButton) = dao.deleteButton(button)

    fun getMacros(profileId: String): Flow<List<Macro>> = dao.getMacrosForProfile(profileId)
    suspend fun saveMacro(macro: Macro) = dao.insertMacro(macro)
    suspend fun softDeleteMacro(macroId: String) = dao.softDeleteMacro(macroId)

    fun getGestures(profileId: String): Flow<List<GestureDef>> = dao.getGesturesForProfile(profileId)
    suspend fun getGesture(gestureId: String): GestureDef? = dao.getGestureById(gestureId)
    suspend fun saveGesture(gesture: GestureDef) = dao.insertGesture(gesture)

    fun getSteps(macroId: String): Flow<List<MacroStep>> = dao.getStepsForMacro(macroId)
    suspend fun getStepsList(macroId: String): List<MacroStep> = dao.getStepsForMacroList(macroId)
    suspend fun saveStep(step: MacroStep) = dao.insertStep(step)
    suspend fun saveSteps(steps: List<MacroStep>) = dao.insertSteps(steps)
    suspend fun deleteStep(step: MacroStep) = dao.deleteStep(step)

    suspend fun logActivity(type: String, message: String) {
        dao.insertLog(ActivityLog(type = type, message = message))
    }

    // --- JSON Backup & Restore with Checksum (Section 7.9) ---
    fun exportBackupJson(profile: Profile, macros: List<Macro>, gestures: List<GestureDef>, buttons: List<FloatingButton>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("profileName", profile.name)

        val payload = root.toString()
        val checksum = calculateSha256(payload)
        root.put("checksum", checksum)
        return root.toString(2)
    }

    fun verifyChecksum(jsonStr: String): Boolean {
        return try {
            val obj = JSONObject(jsonStr)
            val expected = obj.optString("checksum")
            obj.remove("checksum")
            val actual = calculateSha256(obj.toString())
            expected == actual
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateSha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
