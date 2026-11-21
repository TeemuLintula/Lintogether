package com.example.lintogether.sub_functions

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalType {
    MAIN, YEAR, MINOR, SUB
}

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val type: GoalType,
    val parentId: Int? = null, // ID of the parent goal
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val deadline: String? = null
)
