package com.example.servicemaintainreminder.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.AppDatabase

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val itemDao = database.itemDao()
        
        val currentTime = System.currentTimeMillis()
        val threeDaysInMs = 3 * 24 * 60 * 60 * 1000L
        
        val items = itemDao.getAllItemsOnce()
        
        items.forEach { item ->
            val timeDiff = item.nextServiceDate - currentTime
            if (timeDiff in 0..threeDaysInMs) {
                showNotification(item.name, "Service needed soon!")
            } else if (timeDiff < 0) {
                showNotification(item.name, "Service is overdue!")
            }
        }

        return Result.success()
    }

    private fun showNotification(itemName: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "service_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Service Reminder", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Service Reminder: $itemName")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(itemName.hashCode(), notification)
    }
}
