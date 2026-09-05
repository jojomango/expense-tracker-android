package com.jojomango.expensetracker.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** SPEC.md §3.6 匯出/匯入。目前只有 v1，日後 schema 有變動就把這個數字往上加。 */
const val CURRENT_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class BackupWallet(
    val id: String,
    val name: String,
    val currency: String,
    val budgetMode: String,
    val budgetAmount: Long?,
    val archived: Boolean,
)

@Serializable
data class BackupTransaction(
    val id: String,
    val walletId: String,
    val type: String,
    val amount: Long,
    val categoryId: String?,
    val date: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupCategory(
    val id: String,
    val name: String,
    val type: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
)

@Serializable
data class BackupSettings(
    val weekStartDay: Int,
    val theme: String,
    val defaultWalletId: String?,
)

@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Long,
    val wallets: List<BackupWallet>,
    val transactions: List<BackupTransaction>,
    val categories: List<BackupCategory>,
    val settings: BackupSettings,
)

enum class ImportMode { REPLACE, MERGE }

/** 匯入的 JSON 不是合法 JSON、或不符合備份格式（TESTCASES.md T4.2.4）。 */
class BackupParseException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/** 匯入檔案的 schemaVersion 比現在支援的還新（T4.2.2）。 */
class BackupSchemaTooNewException(
    message: String,
) : IllegalArgumentException(message)

/** 匯入檔案有參照完整性問題，例如交易指向不存在的錢包/分類（T4.2.8）。 */
class BackupIntegrityException(
    message: String,
) : IllegalArgumentException(message)

private val backupJson = Json { ignoreUnknownKeys = true }

fun encodeBackup(payload: BackupPayload): String = backupJson.encodeToString(BackupPayload.serializer(), payload)

/**
 * 解析備份 JSON。格式錯誤（不是合法 JSON、缺必要欄位）一律轉成
 * [BackupParseException]，不讓底層的 `SerializationException` 直接洩漏出去
 * （T4.2.3／T4.2.4：缺欄位或格式錯誤都要拋出可讀錯誤、不 crash、整批拒絕）。
 */
fun decodeBackup(json: String): BackupPayload =
    try {
        backupJson.decodeFromString(BackupPayload.serializer(), json)
    } catch (e: SerializationException) {
        throw BackupParseException("Invalid backup file: ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        throw BackupParseException("Invalid backup file: ${e.message}", e)
    }

/**
 * 驗證 schema version 與參照完整性。任何一項不合法就拋錯——呼叫端必須在拋錯時
 * 完全不寫入任何資料（原子性，T4.2.3），這裡只負責檢查、不負責寫入。
 */
@Suppress("ThrowsCount") // 3 種獨立的驗證失敗原因（schema 太新/錢包缺失/分類缺失），拆更多函式沒有比較清楚
fun validateBackup(payload: BackupPayload) {
    if (payload.schemaVersion > CURRENT_BACKUP_SCHEMA_VERSION) {
        throw BackupSchemaTooNewException(
            "Backup schema version ${payload.schemaVersion} is newer than supported " +
                "$CURRENT_BACKUP_SCHEMA_VERSION — please update the app",
        )
    }

    val walletIds = payload.wallets.mapTo(mutableSetOf()) { it.id }
    val categoryIds = payload.categories.mapTo(mutableSetOf()) { it.id }
    for (tx in payload.transactions) {
        if (tx.walletId !in walletIds) {
            throw BackupIntegrityException("Transaction ${tx.id} references missing wallet ${tx.walletId}")
        }
        if (tx.categoryId != null && tx.categoryId !in categoryIds) {
            throw BackupIntegrityException("Transaction ${tx.id} references missing category ${tx.categoryId}")
        }
    }
}

/**
 * merge 模式的交易合併：以 `id` 為鍵，衝突時保留 `updatedAt` 較新者
 * （T4.2.5），不衝突的兩邊都保留（T4.2.6）。錢包／分類沒有 `updatedAt`
 * 欄位可比較，merge 時單純由匯入端覆蓋同 id 的既有資料——見呼叫端
 * （`data` 層的 backup service）如何處理。
 */
fun mergeTransactionsById(
    existing: List<BackupTransaction>,
    incoming: List<BackupTransaction>,
): List<BackupTransaction> {
    val byId = existing.associateByTo(LinkedHashMap()) { it.id }
    for (item in incoming) {
        val current = byId[item.id]
        if (current == null || item.updatedAt >= current.updatedAt) {
            byId[item.id] = item
        }
    }
    return byId.values.toList()
}
