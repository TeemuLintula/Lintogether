package com.example.lintogether

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.Room
import com.example.lintogether.sub_functions.Goal
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoalReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "tasks-database"
        )
            .addMigrations(
                MIGRATION_5_8, MIGRATION_6_8, MIGRATION_7_8, MIGRATION_8_9,
                MIGRATION_9_10, MIGRATION_10_11, MIGRATION_9_11, MIGRATION_8_11,
                MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19,
                MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22
            )
            .build()

        val goals = db.taskDao().getAllGoals().first()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = Calendar.getInstance()

        // Tomorrow reminder
        val tomorrow = now.clone() as Calendar
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = dateFormat.format(tomorrow.time)

        // One week reminder
        val oneWeekAway = now.clone() as Calendar
        oneWeekAway.add(Calendar.DAY_OF_YEAR, 7)
        val oneWeekAwayStr = dateFormat.format(oneWeekAway.time)

        goals.filter { !it.isCompleted }.forEach { goal ->
            if (goal.deadline == tomorrowStr) {
                NotificationHelper.sendNotification(
                    applicationContext,
                    "Reminder: Goal \"${goal.name}\" is due tomorrow!",
                    notificationId = goal.id
                )
            }
            if (goal.deadline == oneWeekAwayStr) {
                NotificationHelper.sendNotification(
                    applicationContext,
                    "Reminder: Goal \"${goal.name}\" is due in one week!",
                    notificationId = goal.id + 100000
                )
            }
        }

        return androidx.work.ListenableWorker.Result.success()
    }
}
