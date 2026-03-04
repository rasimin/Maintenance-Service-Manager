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
}
