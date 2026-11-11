package com.joymilk.app.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val localeID = Locale("id", "ID")

    // Simple Date overloads used by screens
    fun formatDate(date: Date): String =
        SimpleDateFormat("dd MMM yyyy", localeID).format(date)

    fun formatDateTime(date: Date): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", localeID).format(date)

    fun formatDateTime(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", localeID)
        return sdf.format(timestamp.toDate())
    }

    fun formatDateShort(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", localeID)
        return sdf.format(timestamp.toDate())
    }

    fun formatDateOnly(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", localeID)
        return sdf.format(timestamp.toDate())
    }

    fun formatMonthYear(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("MMMM yyyy", localeID)
        return sdf.format(timestamp.toDate())
    }

    fun startOfDay(date: Date = Date()): Timestamp {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(cal.time)
    }

    fun endOfDay(date: Date = Date()): Timestamp {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return Timestamp(cal.time)
    }

    fun startOfWeek(): Timestamp {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(cal.time)
    }

    fun endOfWeek(): Timestamp {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            add(Calendar.DAY_OF_WEEK, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return Timestamp(cal.time)
    }

    fun startOfMonth(year: Int = Calendar.getInstance().get(Calendar.YEAR),
                     month: Int = Calendar.getInstance().get(Calendar.MONTH)): Timestamp {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(cal.time)
    }

    fun endOfMonth(year: Int = Calendar.getInstance().get(Calendar.YEAR),
                   month: Int = Calendar.getInstance().get(Calendar.MONTH)): Timestamp {
        val cal = Calendar.getInstance().apply {
            set(year, month, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return Timestamp(cal.time)
    }
}

