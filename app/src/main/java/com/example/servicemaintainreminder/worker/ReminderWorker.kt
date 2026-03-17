package com.example.servicemaintainreminder.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.servicemaintainreminder.MainActivity
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.AppDatabase

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "service_reminder_channel"
        const val CHANNEL_OVERDUE_ID = "service_overdue_channel"
        const val GROUP_UPCOMING = "group_upcoming"
        const val GROUP_OVERDUE = "group_overdue"
    }

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val itemDao = database.itemDao()

        // Ambil threshold dari SharedPreferences (ikut setting user)
        val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val upcomingDaysLimit = prefs.getInt("upcoming_days_limit", 30)
        val limitInMs = upcomingDaysLimit * 24 * 60 * 60 * 1000L

        val currentTime = System.currentTimeMillis()
        val items = itemDao.getAllItemsOnce()

        val upcomingItems = mutableListOf<String>()
        val overdueItems = mutableListOf<String>()

        items.filter { it.isActive }.forEach { item ->
            val timeDiff = item.nextServiceDate - currentTime
            val daysLeft = (timeDiff / (24 * 60 * 60 * 1000L)).toInt()

            when {
                timeDiff < 0 -> {
                    val daysOverdue = (-daysLeft)
                    overdueItems.add("• ${item.name} (${daysOverdue} hari terlambat)")
                }
                daysLeft <= upcomingDaysLimit -> {
                    upcomingItems.add("• ${item.name} ($daysLeft hari lagi)")
                }
            }
        }

        ensureChannelsCreated()

        // Kirim notifikasi grouped untuk upcoming
        if (upcomingItems.isNotEmpty()) {
            sendGroupedNotification(
                ids = upcomingItems.indices.map { it + 1000 },
                titles = upcomingItems,
                groupKey = GROUP_UPCOMING,
                summaryId = 9001,
                summaryTitle = "🔔 ${upcomingItems.size} Service Akan Segera",
                summaryText = "${upcomingItems.size} perangkat perlu servis segera",
                channelId = CHANNEL_ID,
                iconRes = R.drawable.ic_upcoming
            )
        }

        // Kirim notifikasi grouped untuk overdue
        if (overdueItems.isNotEmpty()) {
            sendGroupedNotification(
                ids = overdueItems.indices.map { it + 2000 },
                titles = overdueItems,
                groupKey = GROUP_OVERDUE,
                summaryId = 9002,
                summaryTitle = "⚠️ ${overdueItems.size} Service Terlambat!",
                summaryText = "${overdueItems.size} perangkat sudah melewati jadwal servis",
                channelId = CHANNEL_OVERDUE_ID,
                iconRes = R.drawable.ic_schedule_fixed
            )
        }

        return Result.success()
    }

    private fun ensureChannelsCreated() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val upcomingChannel = NotificationChannel(
            CHANNEL_ID,
            "Upcoming Service Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminder untuk servis yang akan datang"
            enableLights(true)
            enableVibration(true)
        }

        val overdueChannel = NotificationChannel(
            CHANNEL_OVERDUE_ID,
            "Overdue Service Alert",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alert untuk servis yang sudah terlambat"
            enableLights(true)
            enableVibration(true)
        }

        nm.createNotificationChannel(upcomingChannel)
        nm.createNotificationChannel(overdueChannel)
    }

    private fun sendGroupedNotification(
        ids: List<Int>,
        titles: List<String>,
        groupKey: String,
        summaryId: Int,
        summaryTitle: String,
        summaryText: String,
        channelId: String,
        iconRes: Int
    ) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent: tap notifikasi → buka aplikasi
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Kirim setiap item sebagai notifikasi individual dalam group
        titles.forEachIndexed { index, text ->
            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle(text.substringBefore("(").trim().removePrefix("• "))
                .setContentText(text.substringAfter("(").removeSuffix(")").trim())
                .setSmallIcon(iconRes)
                .setGroup(groupKey)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            nm.notify(ids[index], notification)
        }

        // Kirim summary notification (wajib untuk grouped notifications)
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(summaryTitle)
            .setSummaryText(summaryText)
        titles.forEach { inboxStyle.addLine(it) }

        val summaryNotification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(summaryTitle)
            .setContentText(summaryText)
            .setSmallIcon(iconRes)
            .setStyle(inboxStyle)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(summaryId, summaryNotification)
    }
}
