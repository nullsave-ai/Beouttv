package com.beouttv.app.data.favorites

import android.content.Context
import com.beouttv.app.data.model.ClubInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoriteClubManager {

    private const val PREFS_NAME = "beout_favorite_club_prefs"
    private const val KEY_ID = "fav_club_id"
    private const val KEY_NAME = "fav_club_name"
    private const val KEY_LOGO = "fav_club_logo"
    private const val KEY_RANK = "fav_club_rank"
    private const val KEY_PLAYED = "fav_club_played"
    private const val KEY_POINTS = "fav_club_points"
    private const val KEY_LEAGUE_ID = "fav_club_league_id"
    private const val KEY_LEAGUE_NAME = "fav_club_league_name"
    private const val KEY_NOTIF = "fav_club_notif_enabled"

    private const val KEY_WON = "fav_club_won"
    private const val KEY_DRAW = "fav_club_draw"
    private const val KEY_LOST = "fav_club_lost"
    private const val KEY_GOAL_DIFF = "fav_club_goal_diff"
    private const val KEY_STADIUM = "fav_club_stadium"

    private val _favoriteClubFlow = MutableStateFlow<ClubInfo?>(null)
    val favoriteClubFlow: StateFlow<ClubInfo?> = _favoriteClubFlow.asStateFlow()

    private var isLoaded = false

    fun init(context: Context) {
        if (!isLoaded) {
            val club = loadFromPrefs(context)
            _favoriteClubFlow.value = club
            isLoaded = true
        }
    }

    fun getFavoriteClub(context: Context): ClubInfo? {
        init(context)
        return _favoriteClubFlow.value
    }

    fun isClubFavorite(name: String): Boolean {
        val current = _favoriteClubFlow.value ?: return false
        return current.name.equals(name, ignoreCase = true) || name.contains(current.name, ignoreCase = true) || current.name.contains(name, ignoreCase = true)
    }

    fun setFavoriteClub(context: Context, club: ClubInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ID, club.id)
            .putString(KEY_NAME, club.name)
            .putString(KEY_LOGO, club.logoUrl)
            .putInt(KEY_RANK, club.rank)
            .putInt(KEY_PLAYED, club.played)
            .putInt(KEY_POINTS, club.points)
            .putInt(KEY_WON, club.won)
            .putInt(KEY_DRAW, club.draw)
            .putInt(KEY_LOST, club.lost)
            .putInt(KEY_GOAL_DIFF, club.goalDiff)
            .putString(KEY_LEAGUE_ID, club.leagueId)
            .putString(KEY_LEAGUE_NAME, club.leagueName)
            .putString(KEY_STADIUM, club.stadiumName)
            .putBoolean(KEY_NOTIF, club.notificationsEnabled)
            .apply()

        _favoriteClubFlow.value = club
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val current = _favoriteClubFlow.value ?: return
        val updated = current.copy(notificationsEnabled = enabled)
        setFavoriteClub(context, updated)
    }

    fun clearFavoriteClub(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _favoriteClubFlow.value = null
    }

    private fun loadFromPrefs(context: Context): ClubInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_ID, null) ?: return null
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val logo = prefs.getString(KEY_LOGO, "") ?: ""
        val rank = prefs.getInt(KEY_RANK, 1)
        val played = prefs.getInt(KEY_PLAYED, 0)
        val points = prefs.getInt(KEY_POINTS, 0)
        val won = prefs.getInt(KEY_WON, 0)
        val draw = prefs.getInt(KEY_DRAW, 0)
        val lost = prefs.getInt(KEY_LOST, 0)
        val goalDiff = prefs.getInt(KEY_GOAL_DIFF, 0)
        val leagueId = prefs.getString(KEY_LEAGUE_ID, "11") ?: "11"
        val leagueName = prefs.getString(KEY_LEAGUE_NAME, "الدوري الإسباني") ?: "الدوري الإسباني"
        val stadium = prefs.getString(KEY_STADIUM, "") ?: ""
        val notif = prefs.getBoolean(KEY_NOTIF, true)

        return ClubInfo(
            id = id,
            name = name,
            logoUrl = logo,
            rank = rank,
            played = played,
            points = points,
            won = won,
            draw = draw,
            lost = lost,
            goalDiff = goalDiff,
            leagueId = leagueId,
            leagueName = leagueName,
            stadiumName = stadium,
            notificationsEnabled = notif
        )
    }
}
