package com.example.lintogether

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val activeDays: Set<WeekDay>,
    val priority: Int = 1,
    val completedDates: Set<String> = emptySet(),
    val skippedDates: Set<String> = emptySet(),
    val repeatInterval: Int = 1,
    val startDate: String = "",
    val repeatUnit: RepeatUnit = RepeatUnit.WEEK,
    val activeMonthDays: Set<Int> = emptySet(),
    val skippedDay: Boolean = false,
    val isRelapseTracker: Boolean = false
)

enum class WeekDay { Mon, Tue, Wed, Thu, Fri, Sat, Sun }
enum class RepeatUnit { WEEK, MONTH }

fun isTaskActiveOnDate(task: Task, targetDate: Calendar): Boolean {
    if (task.isRelapseTracker) return true

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val weekDay = when (targetDate.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> WeekDay.Mon
        Calendar.TUESDAY -> WeekDay.Tue
        Calendar.WEDNESDAY -> WeekDay.Wed
        Calendar.THURSDAY -> WeekDay.Thu
        Calendar.FRIDAY -> WeekDay.Fri
        Calendar.SATURDAY -> WeekDay.Sat
        Calendar.SUNDAY -> WeekDay.Sun
        else -> WeekDay.Mon
    }
    
    val isActiveOnDay = if (task.repeatUnit == RepeatUnit.WEEK) {
        task.activeDays.contains(weekDay)
    } else {
        task.activeMonthDays.contains(targetDate.get(Calendar.DAY_OF_MONTH))
    }
    
    return isActiveOnDay && isTaskActiveOnWeek(task, targetDate, dateFormat)
}

fun isTaskActiveOnWeek(task: Task, targetDate: Calendar, dateFormat: SimpleDateFormat): Boolean {
    val startCal = Calendar.getInstance()
    try {
        val dateToParse = if (task.startDate.isEmpty()) {
             dateFormat.format(Calendar.getInstance().time)
        } else {
            task.startDate
        }
        val parsed = dateFormat.parse(dateToParse) ?: return true
        startCal.time = parsed
    } catch (_: Exception) {
        return true
    }

    if (task.repeatUnit == RepeatUnit.MONTH) {
        val startMonth = startCal.get(Calendar.MONTH)
        val startYear = startCal.get(Calendar.YEAR)
        val targetMonth = targetDate.get(Calendar.MONTH)
        val targetYear = targetDate.get(Calendar.YEAR)

        val monthsBetween = (targetYear - startYear) * 12 + (targetMonth - startMonth)
        if (monthsBetween < 0) return false
        if (task.repeatInterval > 1 && monthsBetween % task.repeatInterval != 0) return false

        val dayOfMonth = targetDate.get(Calendar.DAY_OF_MONTH)
        return task.activeMonthDays.contains(dayOfMonth)
    }

    if (task.repeatInterval == 0) {
        val weekday = when (targetDate.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> WeekDay.Mon
            Calendar.TUESDAY -> WeekDay.Tue
            Calendar.WEDNESDAY -> WeekDay.Wed
            Calendar.THURSDAY -> WeekDay.Thu
            Calendar.FRIDAY -> WeekDay.Fri
            Calendar.SATURDAY -> WeekDay.Sat
            Calendar.SUNDAY -> WeekDay.Sun
            else -> WeekDay.Mon
        }
        if (!task.activeDays.contains(weekday)) return false

        val checkCal = startCal.clone() as Calendar
        while (true) {
            val day = when (checkCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> WeekDay.Mon
                Calendar.TUESDAY -> WeekDay.Tue
                Calendar.WEDNESDAY -> WeekDay.Wed
                Calendar.THURSDAY -> WeekDay.Thu
                Calendar.FRIDAY -> WeekDay.Fri
                Calendar.SATURDAY -> WeekDay.Sat
                Calendar.SUNDAY -> WeekDay.Sun
                else -> WeekDay.Mon
            }
            if (day == weekday) {
                return targetDate.get(Calendar.YEAR) == checkCal.get(Calendar.YEAR) &&
                        targetDate.get(Calendar.DAY_OF_YEAR) == checkCal.get(Calendar.DAY_OF_YEAR)
            }
            checkCal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    fun getStartOfWeek(cal: Calendar): Calendar {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.setFirstDayOfWeek(Calendar.MONDAY)
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return c
    }

    val startWeek = getStartOfWeek(startCal)
    val targetWeek = getStartOfWeek(targetDate)

    if (targetWeek.before(startWeek)) return false
    if (task.repeatInterval <= 1) return true

    val diffMillis = targetWeek.timeInMillis - startWeek.timeInMillis
    // Add 12 hours buffer to handle Daylight Saving Time changes (e.g. 23h or 25h days)
    val weeksBetween = ((diffMillis + 12 * 60 * 60 * 1000L) / (7 * 24 * 60 * 60 * 1000L)).toInt()

    return weeksBetween % task.repeatInterval == 0
}
