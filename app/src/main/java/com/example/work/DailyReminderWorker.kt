package com.example.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val channelId = "daily_medical_reminders"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات الأدوية والعلاجات"
            val descriptionText = "تذكير يومي بمراجعة التوصيات العلاجية للأمراض المزمنة المحفوظة"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("تذكير طبي يومي")
            .setContentText("لا تنسَ مراجعة توصياتك العلاجية للأمراض المزمنة المحفوظة اليوم.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(applicationContext).notify(1, builder.build())
            }
        } else {
            NotificationManagerCompat.from(applicationContext).notify(1, builder.build())
        }
    }
}
