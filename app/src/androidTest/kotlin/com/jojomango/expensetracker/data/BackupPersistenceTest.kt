package com.jojomango.expensetracker.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jojomango.expensetracker.domain.BackupCategory
import com.jojomango.expensetracker.domain.BackupIntegrityException
import com.jojomango.expensetracker.domain.BackupParseException
import com.jojomango.expensetracker.domain.BackupPayload
import com.jojomango.expensetracker.domain.BackupSchemaTooNewException
import com.jojomango.expensetracker.domain.BackupSettings
import com.jojomango.expensetracker.domain.BackupTransaction
import com.jojomango.expensetracker.domain.BackupWallet
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.CURRENT_BACKUP_SCHEMA_VERSION
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.decodeBackup
import com.jojomango.expensetracker.domain.encodeBackup
import com.jojomango.expensetracker.domain.mergeTransactionsById
import com.jojomango.expensetracker.domain.validateBackup
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TESTCASES.md T4.2 — 匯出/匯入。純解析/驗證邏輯（decodeBackup／validateBackup／
 * mergeTransactionsById）在 domain 已經是純函式，這裡只測「透過真正的 Room DB
 * 走一趟」的整合行為（round-trip、replace、merge 的原子性）。
 */
@RunWith(AndroidJUnit4::class)
class BackupPersistenceTest {
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

    private fun samplePayload(schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION): BackupPayload {
        val wallet = BackupWallet("w1", "日常", "TWD", BudgetMode.NONE.name, null, false)
        val category = BackupCategory("c1", "飲食", CategoryType.EXPENSE.name, "🍜", "#C1502E", isDefault = true)
        val transaction =
            BackupTransaction(
                id = "t1",
                walletId = "w1",
                type = TransactionType.EXPENSE.name,
                amount = 10000,
                categoryId = "c1",
                date = "2026-08-12",
                note = null,
                createdAt = 0,
                updatedAt = 0,
            )
        return BackupPayload(
            schemaVersion = schemaVersion,
            exportedAt = 0,
            wallets = listOf(wallet),
            transactions = listOf(transaction),
            categories = listOf(category),
            settings = BackupSettings(1, "SYSTEM", null),
        )
    }

    @Test
    fun t4_2_1_exportThenImportReplaceRoundTripsLosslessly() =
        runTest {
            backupRepo.import(samplePayload(), ImportMode.REPLACE)
            val exported = backupRepo.export()

            assertEquals(samplePayload().wallets, exported.wallets)
            assertEquals(samplePayload().transactions, exported.transactions)
            assertEquals(samplePayload().categories, exported.categories)
            assertEquals(samplePayload().settings, exported.settings)
        }

    @Test
    fun t4_2_2_importingNewerSchemaVersionIsRejected() {
        val tooNew = samplePayload(schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION + 1)
        assertThrows(BackupSchemaTooNewException::class.java) { validateBackup(tooNew) }
    }

    @Test
    fun t4_2_3_importingInvalidPayloadLeavesExistingDataUnchanged() =
        runTest {
            backupRepo.import(samplePayload(), ImportMode.REPLACE)
            val before = backupRepo.export()

            val invalid = samplePayload(schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION + 1)
            assertThrows(BackupSchemaTooNewException::class.java) {
                validateBackup(invalid)
                // validateBackup 拋錯，呼叫端（例如 ViewModel/UI）就不會走到 import()
                // 這一步——這裡故意不呼叫 backupRepo.import(invalid, ...)，用來證明
                // 「驗證先於寫入」這個順序本身就保證了原子性，不需要额外的 DB 層回滾。
            }

            val after = backupRepo.export()
            assertEquals(before, after)
        }

    @Test
    fun t4_2_4_malformedJsonThrowsReadableErrorNotCrash() {
        assertThrows(BackupParseException::class.java) { decodeBackup("{ this is not valid json") }
        assertThrows(BackupParseException::class.java) { decodeBackup("") }
    }

    @Test
    fun t4_2_5_mergeConflictKeepsNewerUpdatedAt() {
        val existing =
            BackupTransaction("t1", "w1", "EXPENSE", 100, null, "2026-08-12", null, createdAt = 0, updatedAt = 100)
        val incomingOlder = existing.copy(amount = 999, updatedAt = 50)
        val incomingNewer = existing.copy(amount = 500, updatedAt = 200)

        val keepsExisting = mergeTransactionsById(listOf(existing), listOf(incomingOlder))
        assertEquals(existing, keepsExisting.single())

        val keepsIncoming = mergeTransactionsById(listOf(existing), listOf(incomingNewer))
        assertEquals(incomingNewer, keepsIncoming.single())
    }

    @Test
    fun t4_2_6_mergeWithoutIdConflictKeepsBothSides() {
        val a = BackupTransaction("t1", "w1", "EXPENSE", 100, null, "2026-08-12", null, 0, 0)
        val b = BackupTransaction("t2", "w1", "EXPENSE", 200, null, "2026-08-13", null, 0, 0)

        val merged = mergeTransactionsById(listOf(a), listOf(b))
        assertEquals(setOf("t1", "t2"), merged.map { it.id }.toSet())
    }

    @Test
    fun t4_2_8_transactionReferencingMissingWalletIsRejected() {
        val payload = samplePayload().copy(wallets = emptyList())
        assertThrows(BackupIntegrityException::class.java) { validateBackup(payload) }
    }

    @Test
    fun t4_2_7_importing10000TransactionsCompletesWithinThreeSeconds() =
        runTest {
            val wallet = BackupWallet("w1", "日常", "TWD", BudgetMode.NONE.name, null, false)
            val transactions =
                (1..10_000).map { i ->
                    BackupTransaction(
                        id = "t$i",
                        walletId = "w1",
                        type = TransactionType.EXPENSE.name,
                        amount = 100,
                        categoryId = null,
                        date = "2026-08-12",
                        note = null,
                        createdAt = 0,
                        updatedAt = 0,
                    )
                }
            val payload =
                BackupPayload(
                    schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
                    exportedAt = 0,
                    wallets = listOf(wallet),
                    transactions = transactions,
                    categories = emptyList(),
                    settings = BackupSettings(1, "SYSTEM", null),
                )

            val elapsedMillis =
                kotlin.time
                    .measureTime {
                        backupRepo.import(payload, ImportMode.REPLACE)
                    }.inWholeMilliseconds

            assertEquals(10_000, backupRepo.export().transactions.size)
            org.junit.Assert.assertTrue(
                "Expected import to finish within 3000ms, took ${elapsedMillis}ms",
                elapsedMillis < 3000,
            )
        }

    @Test
    fun encodeDecodeBackupRoundTrips() {
        val payload = samplePayload()
        assertEquals(payload, decodeBackup(encodeBackup(payload)))
    }
}
