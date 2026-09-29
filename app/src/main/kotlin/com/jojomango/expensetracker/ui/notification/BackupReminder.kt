package com.jojomango.expensetracker.ui.notification

import android.Manifest
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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.BackupReminderRepository
import com.jojomango.expensetracker.domain.shouldSendBackupReminder
import com.jojomango.expensetracker.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "backup_reminder"
private const val NOTIFICATION_ID = 1
private const val WORK_NAME = "backup_reminder"

/** 通知點下去直接開設定頁的備份區塊，而不是首頁——使用者點通知就是想去備份。 */
const val EXTRA_OPEN_SETTINGS = "com.jojomango.expensetracker.OPEN_SETTINGS"

/**
 * SPEC.md §3.6：「每 7 天提醒一次備份，用 WorkManager 排程本地通知，不是推播」。
 *
 * 每天檢查一次，而不是直接排一個 7 天週期的 work：使用者一匯出，計時就要從那一刻重新
 * 起算，固定 7 天週期做不到這件事。「到底要不要發」完全交給 domain 的
 * [shouldSendBackupReminder]（有單元測試），這裡只負責讀狀態、發通知、記錄發過了。
 */
object BackupReminderScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupReminderWorker>(1, TimeUnit.DAYS).build()
        // KEEP：每次開 app 都會呼叫，已經排過就不要重排，否則每次開 app 都會把「下次檢查」往後推。
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "備份提醒", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "每 7 天提醒你匯出一次備份"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

// WorkManager 自己用反射建立 Worker，沒辦法走 Hilt 的建構子注入（那需要 hilt-work，
// 不在 SPEC.md §5 的套件清單內），所以用 hilt-android 本來就有的 EntryPoint 拿單例。
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupReminderEntryPoint {
    fun backupReminderRepository(): BackupReminderRepository
}

class BackupReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository =
            EntryPointAccessors
                .fromApplication(applicationContext, BackupReminderEntryPoint::class.java)
                .backupReminderRepository()
        val now = Clock.System.now()
        // 通知發不出去（使用者關掉了通知）就不記錄「提醒過了」——等使用者之後打開通知，
        // 下一次檢查就會馬上提醒，而不是再等 7 天。
        if (shouldSendBackupReminder(repository.get(), now) && showNotification(applicationContext)) {
            repository.recordReminder(now)
        }
        return Result.success()
    }
}

private fun showNotification(context: Context): Boolean {
    val hasPermission =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val manager = NotificationManagerCompat.from(context)
    if (!hasPermission || !manager.areNotificationsEnabled()) return false

    val openSettings =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_SETTINGS, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val pendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            openSettings,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    val notification =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_backup)
            .setContentTitle("該備份資料了")
            .setContentText("距離上次備份已經超過 7 天，建議匯出備份以免資料遺失。")
            .setStyle(NotificationCompat.BigTextStyle().bigText("距離上次備份已經超過 7 天，建議匯出備份以免資料遺失。"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    manager.notify(NOTIFICATION_ID, notification)
    return true
}
