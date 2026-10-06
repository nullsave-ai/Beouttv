package com.beouttv.app.data.ghost

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class UserProfile(
    val username: String = "مستخدم بي اوت",
    val avatarId: Int = 8,
    val memberSince: String = "2026",
    val isEncryptedMode: Boolean = true
)

typealias GhostProfile = UserProfile

object UserAccountManager {

    private const val PREFS_NAME = "beout_user_account_prefs"
    private const val KEY_USERNAME = "user_username"
    private const val KEY_AVATAR = "user_avatar"

    val RANDOM_NAMES = listOf(
        "نجم بي اوت", "قناص الأهداف", "نجم الشاشة", "سيد المغامرة",
        "كابتن بي اوت", "سينما ماستر", "الأسطورة", "صقر الملاعب",
        "متابع الدراما", "نجم الأكشن", "مشاهد ذهبي", "أمير السينما",
        "Cinema King", "Beout Star", "Movie Buff", "Cinema Falcon",
        "Golden Viewer", "Sports Ace", "Action Master", "Screen Legend"
    )

    private val _profileState = MutableStateFlow(UserProfile())
    val profileState: StateFlow<UserProfile> = _profileState.asStateFlow()

    fun getRandomName(): String {
        return RANDOM_NAMES[Random.nextInt(RANDOM_NAMES.size)]
    }

    fun loadProfile(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profile = UserProfile(
            username = prefs.getString(KEY_USERNAME, "مستخدم بي اوت") ?: "مستخدم بي اوت",
            avatarId = prefs.getInt(KEY_AVATAR, 8),
            memberSince = "2026",
            isEncryptedMode = true
        )
        _profileState.value = profile
        return profile
    }

    fun saveProfile(context: Context, username: String, avatarId: Int) {
        val cleanName = username.trim().ifEmpty { "مستخدم بي اوت" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USERNAME, cleanName)
            .putInt(KEY_AVATAR, avatarId)
            .apply()

        _profileState.value = UserProfile(
            username = cleanName,
            avatarId = avatarId,
            memberSince = "2026",
            isEncryptedMode = true
        )
    }

    fun updateAvatar(context: Context, avatarId: Int) {
        val current = _profileState.value
        saveProfile(context, current.username, avatarId)
    }
}

val GhostAccountManager = UserAccountManager
