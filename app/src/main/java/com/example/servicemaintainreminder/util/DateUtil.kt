package com.example.servicemaintainreminder.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun getNextServiceDate(lastServiceDate: Long, intervalValue: Int, intervalUnit: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastServiceDate
        if (intervalUnit == "Months") {
            calendar.add(Calendar.MONTH, intervalValue)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, intervalValue)
        }
        return calendar.timeInMillis
    }

    fun getDaysDifference(targetTimestamp: Long): Int {
        val target = Calendar.getInstance().apply {
            timeInMillis = targetTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = target.timeInMillis - today.timeInMillis
        return (diffMillis / (24 * 60 * 60 * 1000)).toInt()
    }
}
