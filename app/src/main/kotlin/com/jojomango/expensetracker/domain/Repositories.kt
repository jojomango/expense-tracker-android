package com.jojomango.expensetracker.domain

import kotlinx.coroutines.flow.Flow

/**
 * Repository 介面放在 `domain`，實作放在 `data`（見 CLAUDE.md 目錄職責）。
 * 這幾個介面本身只描述「需要什麼能力」，不知道底層是 Room 還是別的東西。
 */
interface WalletRepository {
    fun observeWallets(): Flow<List<Wallet>>

    suspend fun getAllOnce(): List<Wallet>

    suspend fun getWallet(id: String): Wallet?

    suspend fun upsert(wallet: Wallet)

    /** 刪除最後一個錢包會拋出 [LastWalletException]（SPEC.md §3.1）。 */
    suspend fun delete(id: String)
}

interface TransactionRepository {
    fun observeByWallet(walletId: String): Flow<List<Transaction>>

    /** 跨所有錢包——UI-SPEC.md §7 的錢包切換 sheet 需要同時顯示每個錢包各自的
     * 當期餘額，不能只看目前選中的錢包。 */
    fun observeAll(): Flow<List<Transaction>>

    suspend fun getAllOnce(): List<Transaction>

    suspend fun upsert(transaction: Transaction)

    suspend fun delete(id: String)
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    suspend fun getAllOnce(): List<Category>

    /** 系統預設分類會拋出 [DefaultCategoryException]（SPEC.md §3.3）；成功刪除時，
     * 該分類名下的交易一併轉移到「未分類」。 */
    suspend fun delete(id: String)

    suspend fun upsert(category: Category)

    /** 首次啟動建立預設分類——只在目前一個分類都沒有時才建立（SPEC.md §3.3）。 */
    suspend fun seedDefaultsIfEmpty()
}

interface SettingsRepository {
    fun observe(): Flow<Settings>

    suspend fun get(): Settings

    suspend fun update(settings: Settings)
}

/** SPEC.md §3.6 匯出/匯入的統一入口。 */
interface BackupRepository {
    suspend fun export(): BackupPayload

    /** 驗證失敗時拋出（見 `Backup.kt` 的三種例外），現有資料完全不變（原子性）。 */
    suspend fun import(
        payload: BackupPayload,
        mode: ImportMode,
    )
}
