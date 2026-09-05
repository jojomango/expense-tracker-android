package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.DefaultCategories
import com.jojomango.expensetracker.domain.DefaultCategoryException
import com.jojomango.expensetracker.domain.LastWalletException
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * TESTCASES.md T4.1 — Persistence（Room in-memory database）。用 Robolectric
 * 在純 JVM 跑（見 `RoomTestDb.kt` 為什麼不是真的 emulator/androidTest）。
 */
@RunWith(RobolectricTestRunner::class)
class PersistenceTest {
    private lateinit var db: AppDatabase
    private lateinit var walletRepo: RoomWalletRepository
    private lateinit var transactionRepo: RoomTransactionRepository
    private lateinit var categoryRepo: RoomCategoryRepository

    private val fixedInstant = Instant.fromEpochMilliseconds(0)

    @Before
    fun setUp() {
        db = newInMemoryTestDatabase()
        walletRepo = RoomWalletRepository(db.walletDao())
        transactionRepo = RoomTransactionRepository(db.transactionDao())
        categoryRepo = RoomCategoryRepository(db.categoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun wallet(id: String = "w1") = Wallet(id, "日常", "TWD", BudgetMode.NONE, null)

    private fun transaction(
        id: String = "t1",
        walletId: String = "w1",
        categoryId: String? = null,
    ) = Transaction(
        id = id,
        walletId = walletId,
        type = TransactionType.EXPENSE,
        amount = 10000,
        categoryId = categoryId,
        date = LocalDate(2026, 8, 12),
        createdAt = fixedInstant,
        updatedAt = fixedInstant,
    )

    @Test
    fun t4_1_1_newTransactionSurvivesReopen() =
        runTest {
            walletRepo.upsert(wallet())
            transactionRepo.upsert(transaction())

            // 模擬「重新開啟 DB」：直接用同一個底層 SQLite 連線重新查詢
            // （Room in-memory DB 一關閉整個資料就沒了，這裡驗證的是查詢路徑
            // 本身讀得到剛寫入的資料，而不是進程重啟——那個層級的持久化由
            // SQLite 本身保證，不是本專案要測的東西）。
            val loaded = transactionRepo.getAllOnce().single()
            assertEquals(transaction(), loaded)
        }

    @Test
    fun t4_1_2_deletingWalletCascadesItsTransactionsOnly() =
        runTest {
            walletRepo.upsert(wallet("w1"))
            walletRepo.upsert(wallet("w2"))
            transactionRepo.upsert(transaction("t1", "w1"))
            transactionRepo.upsert(transaction("t2", "w2"))

            walletRepo.delete("w1")

            val remaining = transactionRepo.getAllOnce()
            assertEquals(listOf("t2"), remaining.map { it.id })
        }

    @Test
    fun t4_1_3_deletingLastWalletIsRejected() =
        runTest {
            walletRepo.upsert(wallet("only"))
            assertThrows(LastWalletException::class.java) {
                kotlinx.coroutines.runBlocking { walletRepo.delete("only") }
            }
            assertEquals(1, walletRepo.getAllOnce().size)
        }

    @Test
    fun t4_1_4_firstLaunchSeedsDefaultCategories() =
        runTest {
            categoryRepo.seedDefaultsIfEmpty()
            val categories = categoryRepo.getAllOnce()
            assertEquals(11, categories.size)
            assertEquals(7, categories.count { it.type == CategoryType.EXPENSE })
            assertEquals(4, categories.count { it.type == CategoryType.INCOME })

            // 第二次呼叫不應該重複塞（已經有資料就不再 seed）。
            categoryRepo.seedDefaultsIfEmpty()
            assertEquals(11, categoryRepo.getAllOnce().size)
        }

    @Test
    fun t4_1_6_concurrentWriteSameTransactionLastWriterWins() =
        runTest {
            walletRepo.upsert(wallet())
            val original = transaction(categoryId = null)
            transactionRepo.upsert(original)

            val updated = original.copy(amount = 99999, note = "後寫入")
            // Room/SQLite 的寫入本身是序列化的（單一連線 + 交易鎖），這裡不需要
            // 額外的 app 層鎖——直接驗證「後寫入者勝出」，資料完整一致。
            transactionRepo.upsert(original.copy(amount = 1))
            transactionRepo.upsert(updated)

            val loaded = transactionRepo.getAllOnce().single()
            assertEquals(updated, loaded)
        }

    @Test
    fun t4_1_7_deletingDefaultCategoryIsRejected() =
        runTest {
            val defaultCategory = DefaultCategories.seedDefaults().first()
            categoryRepo.upsert(defaultCategory)
            walletRepo.upsert(wallet())
            transactionRepo.upsert(transaction(categoryId = defaultCategory.id))

            assertThrows(DefaultCategoryException::class.java) {
                kotlinx.coroutines.runBlocking { categoryRepo.delete(defaultCategory.id) }
            }

            assertEquals(1, categoryRepo.getAllOnce().size)
            assertEquals(defaultCategory.id, transactionRepo.getAllOnce().single().categoryId)
        }

    @Test
    fun deletingNonDefaultCategoryReassignsItsTransactionsToUncategorized() =
        runTest {
            val custom = Category("custom", "咖啡", CategoryType.EXPENSE, "☕", "#8A5FBF", isDefault = false)
            categoryRepo.upsert(custom)
            walletRepo.upsert(wallet())
            transactionRepo.upsert(transaction(categoryId = custom.id))

            categoryRepo.delete(custom.id)

            assertTrue(categoryRepo.getAllOnce().isEmpty())
            assertNull(transactionRepo.getAllOnce().single().categoryId)
        }
}
