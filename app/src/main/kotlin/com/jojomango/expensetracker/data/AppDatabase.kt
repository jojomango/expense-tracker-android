package com.jojomango.expensetracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Schema version 從 1 開始（SPEC.md §3.5/Phase 3）。之後每一次結構變動都必須
 * 新增一個 `Migration`（見 [Migrations]），不得直接改動既有 `@Entity` 而不寫遷移。
 */
@Database(
    entities = [WalletEntity::class, TransactionEntity::class, CategoryEntity::class, SettingsEntity::class],
    // v2（Phase 10）：categories 新增 defaultKey，見 Migrations.MIGRATION_1_2
    version = 2,
    // schema JSON 輸出到 app/schemas（進版控），遷移測試用它建出舊版本的資料庫。
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun settingsDao(): SettingsDao
}
