package com.example.bataanexplorer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val destination = intent.getStringExtra("PLAN_TITLE") ?: "Your Bataan Trip"
        val channelId = "bataan_travel_reminders"

        // Gamitin ang System time o Intent Extra requestCode para manatiling unique ang notification
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", System.currentTimeMillis().toInt())

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Gumawa ng Notification Channel para sa Android 8.0 (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Travel Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming Bataan travel plans"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Target Intent kapag klinick ng user ang Notification
        val openAppIntent = Intent(context, profile::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // Gamitin ang notificationId para unique ang bawat PendingIntent
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. I-build ang Push Notification UI
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Pwede rin R.drawable.ic_notification kung may custom vector icon
            .setContentTitle("Upcoming Bataan Trip Reminder! 🏖️")
            .setContentText("Get ready! Your scheduled trip to $destination is coming up soon.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Get ready! Your scheduled trip to $destination is coming up soon. Check your itinerary in the app now!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // 4. Android 13 (TIRAMISU) Permission Safety Check bago mag-notify
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                notificationManager.notify(notificationId, builder.build())
            }
        } else {
            notificationManager.notify(notificationId, builder.build())
        }
    }
}