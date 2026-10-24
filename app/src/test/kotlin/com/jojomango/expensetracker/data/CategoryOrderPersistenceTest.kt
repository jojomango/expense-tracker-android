package com.jojomango.expensetracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.jojomango.expensetracker.domain.BackupCategory
import com.jojomango.expensetracker.domain.BackupPayload
import com.jojomango.expensetracker.domain.BackupSettings
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.OTHER_CATEGORY_SORT_ORDER
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TESTCASES.md T6.4.7～T6.4.9 — 分類順序的資料升級與備份（SPEC.md §3.3 v2.5）。 */
@RunWith(RobolectricTestRunner::class)
class CategoryOrderPersistenceTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun `T6 4 7 — v2 升級到 v3：預設分類照代號、自訂分類接在預設之後、其他在最後`() {
        val name = "order-migration"
        helper.createDatabase(name, 2).use { db ->
            fun insert(row: String) = db.execSQL("INSERT INTO categories (id, name, type, icon, color, isDefault, defaultKey) VALUES $row")
            insert("('food', '飲食', 'EXPENSE', '🍜', '#C1502E', 1, 'food')")
            insert("('medical', '醫療', 'EXPENSE', '💊', '#C04A6E', 1, 'medical')")
            insert("('other', '其他', 'EXPENSE', '📦', '#7A7A80', 1, 'other_expense')")
            insert("('pet', '寵物', 'EXPENSE', '🐶', '#123456', 0, NULL)")
        }
        val db = helper.runMigrationsAndValidate(name, 3, true, *Migrations.all)
        val orders =
            db.query("SELECT id, sortOrder FROM categories").use { c ->
                buildMap { while (c.moveToNext()) put(c.getString(0), c.getInt(1)) }
            }
        assertEquals(0, orders["food"])
        assertEquals(5, orders["medical"])
        assertEquals(6, orders["pet"])
        assertEquals(OTHER_CATEGORY_SORT_ORDER, orders["other"])
    }

    @Test
    fun `T6 4 8 — 匯入沒有 sortOrder 的舊備份：依同一條規則補值`() =
        runTest {
            val db = newInMemoryTestDatabase()
            try {
                val old =
                    BackupPayload(
                        schemaVersion = 1,
                        exportedAt = 0,
                        wallets = emptyList(),
                        transactions = emptyList(),
                        categories =
                            listOf(
                                BackupCategory(
                                    "other",
                                    "其他",
                                    CategoryType.EXPENSE.name,
                                    "📦",
                                    "#7A7A80",
                                    isDefault = true,
                                    defaultKey = "other_expense",
                                ),
                                BackupCategory("pet", "寵物", CategoryType.EXPENSE.name, "🐶", "#123456", isDefault = false),
                                BackupCategory("food", "飲食", CategoryType.EXPENSE.name, "🍜", "#C1502E", isDefault = true),
                            ),
                        settings = BackupSettings(1, "SYSTEM", null),
                    )
                RoomBackupRepository(db).import(old, ImportMode.REPLACE)
                val orders = db.categoryDao().getAllOnce().associate { it.id to it.sortOrder }
                assertEquals(0, orders["food"])
                assertEquals(6, orders["pet"])
                assertEquals(OTHER_CATEGORY_SORT_ORDER, orders["other"])
                // 讀出來的順序就是顯示順序
                assertEquals(listOf("food", "pet", "other"), db.categoryDao().getAllOnce().map { it.id })
            } finally {
                db.close()
            }
        }

    @Test
    fun `T6 4 9 — 匯出後立即匯入（replace）：sortOrder 不變、schemaVersion 仍為 1`() =
        runTest {
            val db = newInMemoryTestDatabase()
            try {
                RoomCategoryRepository(db.categoryDao()).seedDefaultsIfEmpty()
                val repo = RoomBackupRepository(db)
                val exported = repo.export()
                assertEquals(1, exported.schemaVersion)
                repo.import(exported, ImportMode.REPLACE)
                assertEquals(
                    exported.categories.associate { it.id to it.sortOrder },
                    repo.export().categories.associate {
                        it.id to
                            it.sortOrder
                    },
                )
                assertEquals(OTHER_CATEGORY_SORT_ORDER, exported.categories.first { it.defaultKey == "other_income" }.sortOrder)
            } finally {
                db.close()
            }
        }
}
