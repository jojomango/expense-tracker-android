package com.jojomango.expensetracker.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.days

/** SPEC.md §3.6「每 7 天提醒一次備份」。 */
val BACKUP_REMINDER_INTERVAL = 7.days

/**
 * 備份提醒需要的時間點。這是**這台裝置**的狀態，不是使用者資料——不進 Room、也不進
 * 備份檔：換了新裝置、從備份還原之後，新裝置應該從自己的第一次啟動重新計時。
 *
 * @property firstLaunchAt 這台裝置第一次啟動 app 的時間；`null` 代表還沒記錄過。
 * @property lastBackupAt 最近一次**成功**匯出備份的時間；`null` 代表從未備份過。
 * @property lastRemindedAt 最近一次真的發出備份通知的時間，只用來節流通知。
 */
data class BackupReminderState(
    val firstLaunchAt: Instant?,
    val lastBackupAt: Instant?,
    val lastRemindedAt: Instant? = null,
)

/**
 * 距離上次備份（沒備份過就是第一次啟動）是否已滿 7 天。邏輯逐條對應網頁版
 * `shouldRemindBackup`：兩個時間點都沒有就不提醒，那代表還沒走過首次啟動流程，
 * 不該提醒使用者備份不存在的資料。設定頁的提醒 banner 用這個判斷。
 */
fun isBackupDue(
    state: BackupReminderState,
    now: Instant,
): Boolean {
    val baseline = state.lastBackupAt ?: state.firstLaunchAt ?: return false
    return now - baseline >= BACKUP_REMINDER_INTERVAL
}

/**
 * 要不要發系統通知。跟 [isBackupDue] 的差別是多一層節流：WorkManager 每天檢查一次，
 * 使用者一直不備份的話，只看 [isBackupDue] 會變成滿 7 天後天天跳通知，不符合
 * 「每 7 天提醒一次」。網頁版沒有這個問題，因為它的提醒是頁內 banner。
 */
fun shouldSendBackupReminder(
    state: BackupReminderState,
    now: Instant,
): Boolean {
    val lastReminded = state.lastRemindedAt
    return isBackupDue(state, now) && (lastReminded == null || now - lastReminded >= BACKUP_REMINDER_INTERVAL)
}

/** SPEC.md §3.6 的備份檔名 `expense-backup-YYYYMMDD-HHmm.json`，用裝置的本地時間。 */
fun backupFileName(localDateTime: LocalDateTime): String {
    fun Int.pad2() = toString().padStart(2, '0')
    val date = "${localDateTime.year}${localDateTime.monthNumber.pad2()}${localDateTime.dayOfMonth.pad2()}"
    val time = "${localDateTime.hour.pad2()}${localDateTime.minute.pad2()}"
    return "expense-backup-$date-$time.json"
}
