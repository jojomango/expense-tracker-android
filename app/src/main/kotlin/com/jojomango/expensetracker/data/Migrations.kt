package com.jojomango.expensetracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jojomango.expensetracker.domain.DefaultCategoryKey

/**
 * Room schema 遷移。每一次結構變動：`@Entity` 改好、`AppDatabase.version` +1、這裡加一個 `Migration`，
 * 並用 `MigrationTestHelper`（app/schemas 裡的舊版 JSON）測試資料真的完整可讀，不是只確認沒有拋例外。
 */
internal object Migrations {
    /**
     * v1 → v2（Phase 10，SPEC.md §3.9）：categories 新增 `defaultKey`。名稱與內建繁中名稱、收支類型都對得上的
     * 預設分類補上代號（T10.3.1）；改過名的預設分類與自訂分類維持 `NULL`（T10.3.2）。
     */
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN defaultKey TEXT")
                DefaultCategoryKey.entries.forEach { key ->
                    db.execSQL(
                        "UPDATE categories SET defaultKey = ? WHERE isDefault = 1 AND type = ? AND name = ?",
                        arrayOf(key.id, key.type.name, LegacyDefaultCategoryNames.nameOf(key)),
                    )
                }
            }
        }

    val all: Array<Migration> = arrayOf(MIGRATION_1_2)
}
