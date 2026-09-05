package com.jojomango.expensetracker.data

import androidx.room.migration.Migration

/**
 * 目前 schema 只有 v1，還沒有任何真正需要的遷移——這個檔案是給下一次真的要
 * 改 schema 時抄的骨架（TASKS.md Phase 3「Room schema v1 → v2 migration...
 * 用 Room 的 MigrationTestHelper 寫骨架」）。
 *
 * 範例：假設 Phase 4+ 想替 `transactions.note` 加一個全文檢索用的索引，
 * 寫法會像這樣（純示範，目前沒有實際用到）：
 *
 * ```kotlin
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(connection: SQLiteConnection) {
 *         connection.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_note ON transactions(note)")
 *     }
 * }
 * ```
 *
 * 真的新增 migration 時：
 * 1. 把新版本的 `@Entity` 改好，`AppDatabase` 的 `version` +1
 * 2. 在這裡新增對應的 `Migration(oldVersion, newVersion)`，並在
 *    `Room.databaseBuilder(...).addMigrations(...)` 註冊
 * 3. 用 `androidx.room:room-testing` 的 `MigrationTestHelper` 寫一個測試：
 *    建出舊版本 schema、塞資料、跑 migrate()、確認資料在新版本 schema 下
 *    完整可讀（不是只確認「沒有拋例外」）
 */
internal object Migrations {
    /** 目前沒有任何已註冊的 migration；`AppDatabase.version` 還停在 1。 */
    val all: Array<Migration> = emptyArray()
}
