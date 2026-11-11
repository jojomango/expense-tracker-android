package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.BackupCategory
import com.jojomango.expensetracker.domain.BackupIntegrityException
import com.jojomango.expensetracker.domain.BackupPayload
import com.jojomango.expensetracker.domain.BackupSettings
import com.jojomango.expensetracker.domain.BackupTransaction
import com.jojomango.expensetracker.domain.BackupWallet
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.CURRENT_BACKUP_SCHEMA_VERSION
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TESTCASES.md T4.2.9～T4.2.11 — 多幣別的備份格式（SPEC.md §3.6 v2.12，schemaVersion 2）。 */
@RunWith(RobolectricTestRunner::class)
class MultiCurrencyBackupTest {
    private lateinit var db: AppDatabase
    private lateinit var backupRepo: RoomBackupRepository

    @Before
    fun setUp() {
        db = newInMemoryTestDatabase()
        backupRepo = RoomBackupRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tx(
        id: String,
        amount: Long,
        currency: String?,
    ) = BackupTransaction(
        id = id,
        walletId = "cebu",
        type = TransactionType.EXPENSE.name,
        amount = amount,
        currency = currency,
        categoryId = "c1",
        date = "2026-11-11",
        note = null,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun payload(
        schemaVersion: Int,
        vararg transactions: BackupTransaction,
    ) = BackupPayload(
        schemaVersion = schemaVersion,
        exportedAt = 0,
        wallets = listOf(BackupWallet("cebu", "宿霧", "PHP", BudgetMode.NONE.name, null, false)),
        transactions = transactions.toList(),
        categories =
            listOf(
                BackupCategory(
                    "c1",
                    "飲食",
                    CategoryType.EXPENSE.name,
                    "🍜",
                    "#C1502E",
                    isDefault = true,
                    defaultKey = "food",
                    sortOrder = 0,
                ),
            ),
        settings = BackupSettings(1, "SYSTEM", null),
    )

    @Test
    fun `T4 2 9 — 多幣別錢包匯出再匯入，每筆交易的幣別一致，schemaVersion 為 2`() =
        runTest {
            val original = payload(2, tx("t1", 120_000, "PHP"), tx("t2", 4_000, "USD"))
            backupRepo.import(original, ImportMode.REPLACE)
            val exported = backupRepo.export()

            assertEquals(2, CURRENT_BACKUP_SCHEMA_VERSION)
            assertEquals(2, exported.schemaVersion)
            assertEquals(original.transactions.sortedBy { it.id }, exported.transactions.sortedBy { it.id })
        }

    @Test
    fun `T4 2 10 — 匯入 schemaVersion 1 的檔，交易幣別補為所屬錢包的幣別`() =
        runTest {
            backupRepo.import(payload(1, tx("t1", 120_000, null)), ImportMode.REPLACE)
            assertEquals(listOf("PHP"), backupRepo.export().transactions.map { it.currency })
        }

    @Test
    fun `T4 2 11 — version 2 的交易缺少幣別或不是內建幣別，整批拒絕，現有資料不變`() =
        runTest {
            backupRepo.import(payload(2, tx("keep", 500, "PHP")), ImportMode.REPLACE)

            assertThrows(BackupIntegrityException::class.java) {
                kotlinx.coroutines.runBlocking { backupRepo.import(payload(2, tx("t1", 100, null)), ImportMode.REPLACE) }
            }
            assertThrows(BackupIntegrityException::class.java) {
                kotlinx.coroutines.runBlocking { backupRepo.import(payload(2, tx("t2", 100, "XYZ")), ImportMode.REPLACE) }
            }
            assertEquals(listOf("keep"), backupRepo.export().transactions.map { it.id })
        }
}
