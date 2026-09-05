package com.jojomango.expensetracker.data

import androidx.room.withTransaction
import com.jojomango.expensetracker.domain.BackupCategory
import com.jojomango.expensetracker.domain.BackupPayload
import com.jojomango.expensetracker.domain.BackupRepository
import com.jojomango.expensetracker.domain.BackupSettings
import com.jojomango.expensetracker.domain.BackupTransaction
import com.jojomango.expensetracker.domain.BackupWallet
import com.jojomango.expensetracker.domain.CURRENT_BACKUP_SCHEMA_VERSION
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.DefaultCategories
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.Settings
import com.jojomango.expensetracker.domain.SettingsRepository
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionRepository
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WalletRepository
import com.jojomango.expensetracker.domain.assertCanDeleteCategory
import com.jojomango.expensetracker.domain.assertCanDeleteWallet
import com.jojomango.expensetracker.domain.mergeTransactionsById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class RoomWalletRepository
    @Inject
    constructor(
        private val dao: WalletDao,
    ) : WalletRepository {
        override fun observeWallets(): Flow<List<Wallet>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun getAllOnce(): List<Wallet> = dao.getAllOnce().map { it.toDomain() }

        override suspend fun getWallet(id: String): Wallet? = dao.getById(id)?.toDomain()

        override suspend fun upsert(wallet: Wallet) = dao.upsert(WalletEntity.fromDomain(wallet))

        override suspend fun delete(id: String) {
            assertCanDeleteWallet(dao.count())
            dao.deleteById(id)
        }
    }

class RoomTransactionRepository
    @Inject
    constructor(
        private val dao: TransactionDao,
    ) : TransactionRepository {
        override fun observeByWallet(walletId: String): Flow<List<Transaction>> =
            dao.observeByWallet(walletId).map { list -> list.map { it.toDomain() } }

        override suspend fun getAllOnce(): List<Transaction> = dao.getAllOnce().map { it.toDomain() }

        override suspend fun upsert(transaction: Transaction) = dao.upsert(TransactionEntity.fromDomain(transaction))

        override suspend fun delete(id: String) = dao.deleteById(id)
    }

class RoomCategoryRepository
    @Inject
    constructor(
        private val dao: CategoryDao,
    ) : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun getAllOnce(): List<Category> = dao.getAllOnce().map { it.toDomain() }

        override suspend fun delete(id: String) {
            val category = dao.getById(id)?.toDomain() ?: return
            assertCanDeleteCategory(category)
            dao.deleteAndReassign(id)
        }

        override suspend fun upsert(category: Category) = dao.upsert(CategoryEntity.fromDomain(category))

        override suspend fun seedDefaultsIfEmpty() {
            if (dao.count() == 0) {
                dao.upsertAll(DefaultCategories.seedDefaults().map { CategoryEntity.fromDomain(it) })
            }
        }
    }

class RoomSettingsRepository
    @Inject
    constructor(
        private val dao: SettingsDao,
    ) : SettingsRepository {
        override fun observe(): Flow<Settings> = dao.observe().map { it?.toDomain() ?: Settings() }

        override suspend fun get(): Settings = dao.get()?.toDomain() ?: Settings()

        override suspend fun update(settings: Settings) = dao.upsert(SettingsEntity.fromDomain(settings))
    }

/**
 * 匯出／匯入的實作。匯入前**先驗證**（`validateBackup` 在呼叫端已經跑過），
 * 這裡假設拿到的 [BackupPayload] 已經合法，只負責原子地寫進資料庫——
 * `replace`/`merge` 都包在同一個 `db.withTransaction` 裡，中途任何一步失敗，
 * 整個 transaction 回滾，現有資料不會變動（T4.2.3 的原子性）。
 */
class RoomBackupRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) : BackupRepository {
        override suspend fun export(): BackupPayload {
            val wallets = db.walletDao().getAllOnce().map { it.toBackup() }
            val transactions = db.transactionDao().getAllOnce().map { it.toBackup() }
            val categories = db.categoryDao().getAllOnce().map { it.toBackup() }
            val settings = (db.settingsDao().get() ?: SettingsEntity.default()).toBackup()
            return BackupPayload(
                schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
                exportedAt =
                    kotlinx.datetime.Clock.System
                        .now()
                        .toEpochMilliseconds(),
                wallets = wallets,
                transactions = transactions,
                categories = categories,
                settings = settings,
            )
        }

        override suspend fun import(
            payload: BackupPayload,
            mode: ImportMode,
        ) {
            db.withTransaction {
                when (mode) {
                    ImportMode.REPLACE -> {
                        db.transactionDao().deleteAll()
                        db.categoryDao().deleteAll()
                        db.walletDao().deleteAll()

                        db.walletDao().upsertAll(payload.wallets.map { it.toEntity() })
                        db.categoryDao().upsertAll(payload.categories.map { it.toEntity() })
                        db.transactionDao().upsertAll(payload.transactions.map { it.toEntity() })
                        db.settingsDao().upsert(payload.settings.toEntity())
                    }
                    ImportMode.MERGE -> {
                        // 錢包／分類沒有 updatedAt 可比較，merge 時匯入端直接覆蓋同 id 的既有資料。
                        db.walletDao().upsertAll(payload.wallets.map { it.toEntity() })
                        db.categoryDao().upsertAll(payload.categories.map { it.toEntity() })

                        val existingTransactions = db.transactionDao().getAllOnce().map { it.toBackup() }
                        val merged = mergeTransactionsById(existingTransactions, payload.transactions)
                        db.transactionDao().upsertAll(merged.map { it.toEntity() })

                        db.settingsDao().upsert(payload.settings.toEntity())
                    }
                }
            }
        }
    }

private fun WalletEntity.toBackup() = BackupWallet(id, name, currency, budgetMode, budgetAmount, archived)

private fun BackupWallet.toEntity() = WalletEntity(id, name, currency, budgetMode, budgetAmount, archived)

private fun CategoryEntity.toBackup() = BackupCategory(id, name, type, icon, color, isDefault)

private fun BackupCategory.toEntity() = CategoryEntity(id, name, type, icon, color, isDefault)

private fun TransactionEntity.toBackup() =
    BackupTransaction(
        id = id,
        walletId = walletId,
        type = type,
        amount = amount,
        categoryId = categoryId,
        date = date.toString(),
        note = note,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
    )

private fun BackupTransaction.toEntity() =
    TransactionEntity(
        id = id,
        walletId = walletId,
        type = type,
        amount = amount,
        categoryId = categoryId,
        date = LocalDate.parse(date),
        note = note,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )

private fun SettingsEntity.toBackup() = BackupSettings(weekStartDay, theme, defaultWalletId)

private fun BackupSettings.toEntity(): SettingsEntity =
    SettingsEntity(
        id = SettingsEntity.SINGLETON_ID,
        weekStartDay = weekStartDay,
        theme = theme,
        defaultWalletId = defaultWalletId,
    )
