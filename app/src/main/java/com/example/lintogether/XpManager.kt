package com.example.lintogether

import android.content.Context
import android.content.SharedPreferences

object XpManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_TOTAL_XP = "total_xp"
    private const val KEY_UNLOCKED_ACHIEVEMENTS = "unlocked_achievements"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTotalXp(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_XP, 0)
    }

    fun addXp(context: Context, amount: Int) {
        val currentXp = getTotalXp(context)
        getPrefs(context).edit().putInt(KEY_TOTAL_XP, currentXp + amount).apply()
    }

    fun subtractXp(context: Context, amount: Int) {
        val currentXp = getTotalXp(context)
        getPrefs(context).edit().putInt(KEY_TOTAL_XP, (currentXp - amount).coerceAtLeast(0)).apply()
    }

    fun getLevelInfo(totalXp: Int): LevelInfo {
        var level = 1
        var remainingXp = totalXp.toDouble()
        var xpRequiredForNext = 10.0
        
        while (remainingXp >= xpRequiredForNext) {
            remainingXp -= xpRequiredForNext
            level++
            xpRequiredForNext *= 1.1
        }
        
        return LevelInfo(level, remainingXp, xpRequiredForNext)
    }

    fun getUnlockedAchievements(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, emptySet()) ?: emptySet()
    }

    fun unlockAchievement(context: Context, achievementId: String) {
        val unlocked = getUnlockedAchievements(context).toMutableSet()
        if (unlocked.add(achievementId)) {
            getPrefs(context).edit().putStringSet(KEY_UNLOCKED_ACHIEVEMENTS, unlocked).apply()
            
            val achievement = AllAchievements.find { it.id == achievementId }
            val reward = achievement?.xpReward ?: 10
            addXp(context, reward)
            
            achievement?.let {
                NotificationHelper.sendNotification(context, "Achievement Unlocked: ${it.name}")
            }
        }
    }
}

data class LevelInfo(
    val level: Int,
    val currentLevelXp: Double,
    val nextLevelXp: Double
) {
    val progress: Float
        get() = (currentLevelXp / nextLevelXp).coerceIn(0.0, 1.0).toFloat()
}

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val xpReward: Int = 10
)

val AllAchievements = listOf(
    Achievement("getting_started", "Getting Started", "Reach 1 completed day"),
    Achievement("real_commitment", "Real commitment", "Reach 7 completed day streak"),
    Achievement("monk_mode", "Monk mode", "Reach 30 completed day streak"),
    Achievement("actual_god", "Actual God", "Reach 90 completed day streak"),
    Achievement("getting_better", "Getting better", "Reach 7 day streak of not relapsing"),
    Achievement("getting_control", "Getting control", "Reach 30 day streak of not relapsing"),
    Achievement("absolute_control_of_mind", "Absolute control of mind", "Reach 90 day streak of not relapsing"),
    Achievement("big_money", "Big money", "Reach net worth of 50 000"),
    Achievement("the_first_hundred", "The first hundred", "Reach net worth of 100 000"),
    Achievement("net_worth_200k", "200", "Reach net worth of 200 000"),
    Achievement("half_way_there", "Half way there", "Reach net worth of 500 000"),
    Achievement("one_million", "One Million", "Reach net worth of 1 000 000"),
    Achievement("self_aware", "Self Aware", "Write your first reflection"),
    Achievement("deep_thinker", "Deep Thinker", "Write 10 reflections"),
    Achievement("visionary", "Visionary", "Create your first major goal"),
    Achievement("achiever", "Achiever", "Mark a major goal as completed"),
    Achievement("routine_is_set", "Routine is set", "Reach a 100 day streak on any task"),
    Achievement("every_single_day", "Every single day", "Reach one year day streak on any task"),
    Achievement("new_years_promise", "New years promise", "Add a new goal in January 1"),
    Achievement("good_gains", "Good gains", "Increase 10% weight on any move"),
    Achievement("gym_rat", "Gym rat", "Increase 50% weight on any move"),
    Achievement("hulk", "Hulk", "Increase weight on any move 100%"),
)
