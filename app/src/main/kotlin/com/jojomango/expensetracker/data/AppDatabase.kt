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
    version = 1,
    // 目前沒有第二個 schema 版本需要比對，先關閉 schema export 避免產生
    // 沒人用的 JSON 檔——真的要寫 v1→v2 migration 時再打開（見 Migrations.kt）。
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun settingsDao(): SettingsDao
}
