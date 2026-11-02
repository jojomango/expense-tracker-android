package com.jojomango.expensetracker.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import com.jojomango.expensetracker.domain.BackupWriteResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.File

private const val AUTHORITY = "test.backup"
private const val LOCATION = "content://$AUTHORITY/backup.json"

/** 模擬使用者選的文件服務；[readFile] 可以和寫入的檔案不同，用來模擬寫壞。 */
class FakeDocumentProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        if (writeFile?.exists() == true) cursor.addRow(arrayOf("backup.json"))
        return cursor
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {
        val file = if (mode == "r") readFile ?: writeFile else writeFile
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun getType(uri: Uri) = "application/json"

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ) = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0

    companion object {
        var writeFile: File? = null
        var readFile: File? = null
    }
}

/** TESTCASES.md T4.6.1～T4.6.3（SPEC.md §3.6 v2.9）。 */
@RunWith(RobolectricTestRunner::class)
class SafBackupFileStoreTest {
    private lateinit var file: File
    private lateinit var store: SafBackupFileStore

    @Before
    fun setUp() {
        file = File.createTempFile("backup", ".json")
        FakeDocumentProvider.writeFile = file
        FakeDocumentProvider.readFile = null
        Robolectric
            .buildContentProvider(FakeDocumentProvider::class.java)
            .create(ProviderInfo().apply { authority = AUTHORITY })
        store = SafBackupFileStore(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `T4_6_1 較短的內容完全取代舊內容`() =
        runTest {
            file.writeText("old content that is much longer")
            assertEquals(BackupWriteResult.SUCCESS, store.write(LOCATION, "new"))
            assertEquals("new", file.readText())
        }

    @Test
    fun `T4_6_2 讀回不一致時回報失敗`() =
        runTest {
            FakeDocumentProvider.readFile = File.createTempFile("other", ".json").apply { writeText("broken") }
            assertEquals(BackupWriteResult.FAILED, store.write(LOCATION, "new"))
        }

    @Test
    fun `T4_6_3 檔案不存在時回報找不到且不建立新檔`() =
        runTest {
            file.delete()
            assertEquals(BackupWriteResult.MISSING, store.write(LOCATION, "new"))
            assertFalse(file.exists())
            assertFalse(store.exists(LOCATION))
            assertNull(store.displayName(LOCATION))
        }

    @Test
    fun `存在的檔案回傳檔名`() =
        runTest {
            assertTrue(store.exists(LOCATION))
            assertEquals("backup.json", store.displayName(LOCATION))
        }
}
