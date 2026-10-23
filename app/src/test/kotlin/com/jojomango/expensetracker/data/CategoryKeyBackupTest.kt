package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.BackupCategory
import com.jojomango.expensetracker.domain.BackupPayload
import com.jojomango.expensetracker.domain.BackupSettings
import com.jojomango.expensetracker.domain.CURRENT_BACKUP_SCHEMA_VERSION
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.DefaultCategoryKey
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.encodeBackup
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TESTCASES.md T10.3.3～T10.3.5 — 備份裡的預設分類代號（SPEC.md §3.9）。 */
@RunWith(RobolectricTestRunner::class)
class CategoryKeyBackupTest {
    private lateinit var db: AppDatabase
    private lateinit var backupRepo: RoomBackupRepository
    private lateinit var categoryRepo: RoomCategoryRepository

    @Before
    fun setUp() {
        db = newInMemoryTestDatabase()
        backupRepo = RoomBackupRepository(db)
        categoryRepo = RoomCategoryRepository(db.categoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `T10 3 3 — 匯出的預設分類帶有代號，schemaVersion 仍為 1`() =
        runTest {
            categoryRepo.seedDefaultsIfEmpty()
            val payload = backupRepo.export()
            assertEquals(1, CURRENT_BACKUP_SCHEMA_VERSION)
            assertEquals(1, payload.schemaVersion)
            assertEquals(DefaultCategoryKey.entries.map { it.id }.toSet(), payload.categories.mapNotNull { it.defaultKey }.toSet())
            assertTrue(encodeBackup(payload).contains("\"defaultKey\":\"food\""))
        }

    @Test
    fun `T10 3 4 — 匯入沒有代號的舊備份，依繁中名稱補上代號`() =
        runTest {
            val old =
                BackupPayload(
                    schemaVersion = 1,
                    exportedAt = 0,
                    wallets = emptyList(),
                    transactions = emptyList(),
                    categories =
                        listOf(
                            BackupCategory("c1", "飲食", CategoryType.EXPENSE.name, "🍜", "#C1502E", isDefault = true),
                            BackupCategory("c2", "吃飯", CategoryType.EXPENSE.name, "🍜", "#C1502E", isDefault = true),
                            BackupCategory("c3", "其他", CategoryType.INCOME.name, "📦", "#7A7A80", isDefault = true),
                        ),
                    settings = BackupSettings(1, "SYSTEM", null),
                )
            backupRepo.import(old, ImportMode.REPLACE)
            val byId = db.categoryDao().getAllOnce().associateBy { it.id }
            assertEquals("food", byId.getValue("c1").defaultKey)
            assertNull(byId.getValue("c2").defaultKey)
            assertEquals("吃飯", byId.getValue("c2").name)
            assertEquals("other_income", byId.getValue("c3").defaultKey)
        }

    @Test
    fun `T10 3 5 — 匯出的備份不包含語言設定`() =
        runTest {
            categoryRepo.seedDefaultsIfEmpty()
            assertFalse(encodeBackup(backupRepo.export()).contains("language", ignoreCase = true))
        }
}
