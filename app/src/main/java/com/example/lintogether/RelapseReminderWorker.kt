package com.example.lintogether

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class RelapseReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "tasks-database"
        ).build()

        val taskDao = db.taskDao()
        val tasks: List<Task> = taskDao.getAllTasksSync()

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val yesterdayStr = dateFormat.format(yesterday.time)

        val relapsedYesterday = tasks.any { task ->
            task.isRelapseTracker && task.completedDates.contains(yesterdayStr)
        }

        if (relapsedYesterday) {
            NotificationHelper.sendNotification(
                applicationContext,
                "Good morning! Today I know you wont relapse.. Unless you are a f'cking loser."
            )
        }

        return Result.success()
    }
}
