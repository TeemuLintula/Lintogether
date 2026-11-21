package com.example.lintogether.sub_functions

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gym_exercises")
data class GymExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gymDay: String, // e.g., "Leg Day", "Push Day"
    val completedDates: Set<String> = emptySet(),
    val position: Int = 0,
    val weight: String = "",
    val weightMarker: String = "", // "", "+", "-"
    val weightHistory: List<WeightEntry> = emptyList()
)

data class WeightEntry(
    val timestamp: Long,
    val weight: Double
)

@Entity(tableName = "gym_days")
data class GymDay(
    @PrimaryKey val name: String,
    val position: Int = 0
)
