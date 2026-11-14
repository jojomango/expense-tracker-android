package com.jojomango.expensetracker.ui.wallet

import androidx.lifecycle.SavedStateHandle
import com.jojomango.expensetracker.data.AppDatabase
import com.jojomango.expensetracker.data.RoomCategoryRepository
import com.jojomango.expensetracker.data.RoomTransactionRepository
import com.jojomango.expensetracker.data.RoomWalletRepository
import com.jojomango.expensetracker.data.newInMemoryTestDatabase
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Settings
import com.jojomango.expensetracker.domain.SettingsRepository
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TESTCASES.md T8.6.6～T8.6.8（UI-SPEC.md §11.1）：檢視頁的取消封存與刪除。 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ArchivedWalletViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var walletRepo: RoomWalletRepository
    private lateinit var transactionRepo: RoomTransactionRepository

    private val settings =
        object : SettingsRepository {
            private val state = MutableStateFlow(Settings())

            override fun observe() = state

            override suspend fun get() = state.value

            override suspend fun update(settings: Settings) {
                state.value = settings
            }
        }

    private fun wallet(
        id: String,
        name: String,
        archived: Boolean = false,
    ) = Wallet(id = id, name = name, currency = "TWD", budgetMode = BudgetMode.NONE, budgetAmount = null, archived = archived)

    private fun tx(
        id: String,
        walletId: String,
    ) = Transaction(
        id = id,
        walletId = walletId,
        type = TransactionType.EXPENSE,
        amount = 100,
        currency = "TWD",
        categoryId = null,
        date = LocalDate(2026, 11, 12),
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = newInMemoryTestDatabase()
        walletRepo = RoomWalletRepository(db.walletDao())
        transactionRepo = RoomTransactionRepository(db.transactionDao())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        ArchivedWalletViewModel(
            savedStateHandle = SavedStateHandle(mapOf("walletId" to "old")),
            walletRepository = walletRepo,
            transactionRepository = transactionRepo,
            categoryRepository = RoomCategoryRepository(db.categoryDao()),
            settingsRepository = settings,
        )

    private suspend fun seed() {
        walletRepo.upsert(wallet("daily", "日常"))
        walletRepo.upsert(wallet("old", "舊帳", archived = true))
        transactionRepo.upsert(tx("d1", "daily"))
        listOf("o1", "o2", "o3").forEach { transactionRepo.upsert(tx(it, "old")) }
    }

    @Test
    fun `T8_6_3 載入封存錢包與它的交易`() =
        runTest(UnconfinedTestDispatcher()) {
            seed()
            val vm = viewModel()
            val job = launch { vm.uiState.collect {} }
            val state = vm.uiState.first { !it.home.isLoading }
            assertEquals("舊帳", state.home.currentWallet?.name)
            assertEquals(3, state.home.transactionGroups.sumOf { it.items.size })
            job.cancel()
        }

    @Test
    fun `T8_6_6 取消封存：錢包回到未封存，結果帶名稱`() =
        runTest(UnconfinedTestDispatcher()) {
            seed()
            val vm = viewModel()
            val job = launch { vm.uiState.collect {} }
            vm.uiState.first { !it.home.isLoading }
            vm.unarchive()
            assertEquals(WalletEditResult.Unarchived("舊帳"), vm.uiState.first { it.result != null }.result)
            assertEquals(false, walletRepo.getWallet("old")?.archived)
            assertEquals(3, transactionRepo.countByWallet("old"))
            job.cancel()
        }

    @Test
    fun `T8_6_7 要刪除時查出交易筆數；取消就關掉`() =
        runTest(UnconfinedTestDispatcher()) {
            seed()
            val vm = viewModel()
            val job = launch { vm.uiState.collect {} }
            vm.uiState.first { !it.home.isLoading }
            vm.requestDelete()
            assertEquals(3, vm.uiState.first { it.pendingDeleteTransactionCount != null }.pendingDeleteTransactionCount)
            vm.dismissDelete()
            assertNull(vm.uiState.first { it.pendingDeleteTransactionCount == null }.pendingDeleteTransactionCount)
            assertEquals("舊帳", walletRepo.getWallet("old")?.name)
            job.cancel()
        }

    @Test
    fun `T8_6_8 確認刪除：錢包與它的交易都刪掉，其他錢包不受影響`() =
        runTest(UnconfinedTestDispatcher()) {
            seed()
            val vm = viewModel()
            val job = launch { vm.uiState.collect {} }
            vm.uiState.first { !it.home.isLoading }
            vm.requestDelete()
            vm.uiState.first { it.pendingDeleteTransactionCount != null }
            vm.confirmDelete()
            assertEquals(WalletEditResult.Deleted("舊帳"), vm.uiState.first { it.result != null }.result)
            assertNull(walletRepo.getWallet("old"))
            assertEquals(0, transactionRepo.countByWallet("old"))
            assertEquals(listOf("d1"), transactionRepo.getAllOnce().map { it.id })
            assertEquals("日常", walletRepo.getWallet("daily")?.name)
            job.cancel()
        }
}
