package com.jojomango.expensetracker.ui.settings

import android.net.Uri
import com.jojomango.expensetracker.data.BackupFileManager
import com.jojomango.expensetracker.data.FakeBackupFiles
import com.jojomango.expensetracker.data.FakeBackupReminders
import com.jojomango.expensetracker.data.FakeBackups
import com.jojomango.expensetracker.domain.BACKUP_FILE_NAME
import com.jojomango.expensetracker.domain.BackupReminderState
import com.jojomango.expensetracker.ui.analytics.AnalyticsConsent
import com.jojomango.expensetracker.ui.analytics.AnalyticsSink
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val REMEMBERED = "content://docs/remembered"
private val LAST_BACKUP = Instant.parse("2026-10-31T04:00:00Z")

/** TESTCASES.md T4.6.4～T4.6.11（SPEC.md §3.6 v2.9）。 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupExportFlowTest {
    private val rememberedState = BackupReminderState(null, LAST_BACKUP, backupFileLocation = REMEMBERED)
    private val requests = mutableListOf<BackupPickerRequest>()

    private fun viewModel(
        reminders: FakeBackupReminders,
        files: FakeBackupFiles = FakeBackupFiles(),
    ) = BackupExportViewModel(
        backupFiles = BackupFileManager(FakeBackups(), reminders, files),
        backupReminderRepository = reminders,
        analyticsTracker =
            AnalyticsTracker(
                object : AnalyticsConsent {
                    override var enabled = false
                },
                object : AnalyticsSink {
                    override fun log(
                        name: String,
                        params: Map<String, Any>,
                    ) = Unit

                    override fun setCollectionEnabled(enabled: Boolean) = Unit
                },
            ),
    )

    private fun runFlow(
        vm: BackupExportViewModel,
        block: suspend () -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val job = launch { vm.backupPickerRequests.collect { requests += it } }
        block()
        job.cancel()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T4_6_4 沒有記住的檔案時開選擇器`() {
        val vm = viewModel(FakeBackupReminders())
        runFlow(vm) { vm.onExportClicked() }
        assertEquals(listOf(BackupPickerRequest(BackupPicker.CHOOSE_FILE, BACKUP_FILE_NAME)), requests)
    }

    @Test
    fun `選擇檔案成功後記住它`() {
        val reminders = FakeBackupReminders()
        val files = FakeBackupFiles()
        val vm = viewModel(reminders, files)
        vm.onBackupFileChosen(Uri.parse("content://docs/new"), enableAuto = false)
        assertEquals("content://docs/new", reminders.state.value.backupFileLocation)
        assertEquals(listOf("content://docs/new"), files.writes)
        assertNotNull(reminders.state.value.lastBackupAt)
    }

    @Test
    fun `T4_6_5 有記住的檔案時先詢問`() {
        val files = FakeBackupFiles()
        val vm = viewModel(FakeBackupReminders(rememberedState), files)
        vm.onExportClicked()
        assertEquals(BackupUpdatePrompt("backup.json", LAST_BACKUP), vm.state.value.updatePrompt)
        assertEquals(emptyList<String>(), files.writes)
    }

    @Test
    fun `T4_6_6 更新備份寫入記住的檔案`() {
        val reminders = FakeBackupReminders(rememberedState.copy(lastBackupAt = null))
        val files = FakeBackupFiles()
        val vm = viewModel(reminders, files)
        vm.onExportClicked()
        vm.onBackupUpdateChoice(BackupUpdateChoice.UPDATE)
        assertEquals(listOf(REMEMBERED), files.writes)
        assertNotNull(reminders.state.value.lastBackupAt)
        assertNull(vm.state.value.updatePrompt)
    }

    @Test
    fun `T4_6_7 另存新檔不改變記住的檔案`() {
        val reminders = FakeBackupReminders(rememberedState)
        val files = FakeBackupFiles()
        val vm = viewModel(reminders, files)
        runFlow(vm) {
            vm.onExportClicked()
            vm.onBackupUpdateChoice(BackupUpdateChoice.SAVE_AS_NEW)
        }
        val request = requests.single()
        assertEquals(BackupPicker.SAVE_COPY, request.picker)
        assertTrue(request.suggestedName, Regex("""expense-backup-\d{8}-\d{4}\.json""").matches(request.suggestedName))
        vm.onBackupCopyChosen(Uri.parse("content://docs/copy"))
        assertEquals(listOf("content://docs/copy"), files.writes)
        assertEquals(REMEMBERED, reminders.state.value.backupFileLocation)
    }

    @Test
    fun `T4_6_8 取消不寫入`() {
        val reminders = FakeBackupReminders(rememberedState)
        val files = FakeBackupFiles()
        val vm = viewModel(reminders, files)
        vm.onExportClicked()
        vm.onBackupUpdateChoice(BackupUpdateChoice.CANCEL)
        assertNull(vm.state.value.updatePrompt)
        assertEquals(emptyList<String>(), files.writes)
        assertEquals(LAST_BACKUP, reminders.state.value.lastBackupAt)
    }

    @Test
    fun `T4_6_9 檔案不見時清除記憶並開選擇器`() {
        val reminders = FakeBackupReminders(rememberedState)
        val vm = viewModel(reminders, FakeBackupFiles(fileExists = false))
        runFlow(vm) { vm.onExportClicked() }
        assertNull(reminders.state.value.backupFileLocation)
        assertNull(vm.state.value.updatePrompt)
        assertEquals(BackupPicker.CHOOSE_FILE, requests.single().picker)
    }

    @Test
    fun `T4_6_10 開啟自動備份沒有檔案時先選檔且未選前維持關閉`() {
        val reminders = FakeBackupReminders()
        val vm = viewModel(reminders)
        runFlow(vm) { vm.setAutoBackupEnabled(true) }
        assertEquals(BackupPicker.ENABLE_AUTO, requests.single().picker)
        assertFalse(reminders.state.value.autoBackupEnabled)
    }

    @Test
    fun `開啟自動備份選好檔案後才開啟`() {
        val reminders = FakeBackupReminders()
        val vm = viewModel(reminders)
        vm.onBackupFileChosen(Uri.parse("content://docs/new"), enableAuto = true)
        assertTrue(reminders.state.value.autoBackupEnabled)
    }

    @Test
    fun `T4_6_11 關閉自動備份保留記住的檔案`() {
        val reminders = FakeBackupReminders(rememberedState.copy(autoBackupEnabled = true))
        val vm = viewModel(reminders)
        vm.setAutoBackupEnabled(false)
        assertFalse(reminders.state.value.autoBackupEnabled)
        assertEquals(REMEMBERED, reminders.state.value.backupFileLocation)
    }

    @Test
    fun `更換檔案時釋放舊檔案的授權`() {
        val reminders = FakeBackupReminders(rememberedState)
        val files = FakeBackupFiles()
        val vm = viewModel(reminders, files)
        vm.onBackupFileChosen(Uri.parse("content://docs/new"), enableAuto = false)
        assertEquals(listOf(REMEMBERED), files.released)
    }
}
