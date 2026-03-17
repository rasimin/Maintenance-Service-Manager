package com.example.servicemaintainreminder.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // When the alarm fires, trigger the ReminderWorker as a one-time task
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
