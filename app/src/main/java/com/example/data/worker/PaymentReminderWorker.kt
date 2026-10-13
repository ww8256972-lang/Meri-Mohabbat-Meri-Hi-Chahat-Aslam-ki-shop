package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.db.AslamDatabase
import kotlinx.coroutines.flow.first

class PaymentReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AslamDatabase.getInstance(context)
            val pendingReminders = db.reminderDao().getPendingReminders().first()
            val now = System.currentTimeMillis()

            val dueOrOverdue = pendingReminders.filter { it.dueDate <= now + (24 * 60 * 60 * 1000) }

            if (dueOrOverdue.isNotEmpty()) {
                sendNotification(dueOrOverdue.size, dueOrOverdue.sumOf { it.amountDue })
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(count: Int, totalDue: Double) {
        val channelId = "aslam_record_payment_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Payment Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for customer due payments and khata follow-ups"
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ASLAM RECORD: $count Payments Due")
            .setContentText("Total ₹${String.format("%.2f", totalDue)} due for collection. Tap to review customer khata.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
