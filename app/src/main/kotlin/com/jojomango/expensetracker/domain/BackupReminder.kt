package com.jojomango.expensetracker.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** SPEC.md §3.6：開始使用滿這個天數，才會在月底提醒備份。 */
const val BACKUP_REMINDER_MIN_DAYS = 30

/**
 * 備份提醒需要的時間點。這是**這台裝置**的狀態，不是使用者資料——不進 Room、也不進
 * 備份檔：換了新裝置、從備份還原之後，新裝置應該從自己的第一次啟動重新計時。
 *
 * @property firstLaunchAt 這台裝置第一次啟動 app 的時間；`null` 代表還沒記錄過。
 * @property lastBackupAt 最近一次**成功**匯出備份的時間；`null` 代表從未備份過。
 * @property lastRemindedAt 最近一次真的發出備份通知的時間，只用來節流通知。
 * @property autoBackupEnabled 使用者是否開啟每月自動備份，預設關閉。
 * @property backupFileLocation App 記住的備份檔位置；`null` 代表還沒有。
 */
data class BackupReminderState(
    val firstLaunchAt: Instant?,
    val lastBackupAt: Instant?,
    val lastRemindedAt: Instant? = null,
    val autoBackupEnabled: Boolean = false,
    val backupFileLocation: String? = null,
)

/**
 * 今天或今天之前最近的一個月底。提醒時機以它為準，而不是「今天是不是月底」：
 * WorkManager 不保證月底當天執行得到，關機或系統延後時要能補發。
 */
private fun latestMonthEnd(today: LocalDate): LocalDate {
    val monthStart = LocalDate(today.year, today.month, 1)
    val monthEnd = monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    return if (today == monthEnd) today else monthStart.minus(1, DateTimeUnit.DAY)
}

private fun Instant.localDate(timeZone: TimeZone): LocalDate = toLocalDateTime(timeZone).date

/**
 * SPEC.md §3.6：最近月底距首次啟動滿 30 天，而且該月開始之後還沒備份過。
 * [BackupReminderState.firstLaunchAt] 是 `null` 代表還沒走過首次啟動流程，不該提醒使用者
 * 備份不存在的資料。設定頁的提示用這個判斷，所以它會一直顯示到使用者備份為止。
 */
fun isBackupDue(
    state: BackupReminderState,
    now: Instant,
    timeZone: TimeZone,
): Boolean {
    val firstLaunch = state.firstLaunchAt?.localDate(timeZone) ?: return false
    val monthEnd = latestMonthEnd(now.localDate(timeZone))
    val monthStart = LocalDate(monthEnd.year, monthEnd.month, 1)
    val usedLongEnough = firstLaunch.plus(BACKUP_REMINDER_MIN_DAYS, DateTimeUnit.DAY) <= monthEnd
    val backedUp = state.lastBackupAt?.localDate(timeZone)?.let { it >= monthStart } ?: false
    return usedLongEnough && !backedUp
}

/**
 * 要不要發系統通知。跟 [isBackupDue] 的差別是多一層節流：WorkManager 每天檢查一次，
 * 只看 [isBackupDue] 的話，使用者不備份就會天天跳通知；同一個月底最多發一次。
 */
fun shouldSendBackupReminder(
    state: BackupReminderState,
    now: Instant,
    timeZone: TimeZone,
): Boolean {
    val lastReminded = state.lastRemindedAt?.localDate(timeZone)
    val alreadyReminded = lastReminded != null && lastReminded >= latestMonthEnd(now.localDate(timeZone))
    return isBackupDue(state, now, timeZone) && !alreadyReminded
}

/**
 * SPEC.md §3.6 v2.9：要不要在這次檢查時自動更新備份檔。不看 `lastRemindedAt`：
 * 通知節流只管通知，失敗後隔天還是要重試。沒有交易時不覆蓋，避免重灌後蓋掉好的備份。
 */
fun shouldAttemptAutoBackup(
    state: BackupReminderState,
    transactionCount: Int,
    now: Instant,
    timeZone: TimeZone,
): Boolean = state.autoBackupEnabled && transactionCount > 0 && isBackupDue(state, now, timeZone)

/** SPEC.md §3.6 v2.9：App 記住並持續更新的備份檔，預設用這個固定檔名。 */
const val BACKUP_FILE_NAME = "expense-backup.json"

/** SPEC.md §3.6 「另存新檔」的檔名 `expense-backup-YYYYMMDD-HHmm.json`，用裝置的本地時間。 */
fun backupFileName(localDateTime: LocalDateTime): String {
    fun Int.pad2() = toString().padStart(2, '0')
    val date = "${localDateTime.year}${localDateTime.monthNumber.pad2()}${localDateTime.dayOfMonth.pad2()}"
    val time = "${localDateTime.hour.pad2()}${localDateTime.minute.pad2()}"
    return "expense-backup-$date-$time.json"
}
