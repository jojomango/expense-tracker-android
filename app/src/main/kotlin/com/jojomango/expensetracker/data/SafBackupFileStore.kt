package com.jojomango.expensetracker.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.jojomango.expensetracker.domain.BackupFileStore
import com.jojomango.expensetracker.domain.BackupWriteResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject

private const val PERSIST_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

/** SPEC.md §3.6 v2.9：用 Storage Access Framework 更新使用者選的備份檔。 */
class SafBackupFileStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BackupFileStore {
        private val resolver get() = context.contentResolver

        override suspend fun write(
            location: String,
            content: String,
        ): BackupWriteResult =
            withContext(Dispatchers.IO) {
                val uri = Uri.parse(location)
                // 檔案不見時不能直接寫：有些服務會重新建立一個使用者不知道的檔案。
                if (nameOf(uri) == null) return@withContext BackupWriteResult.MISSING
                val bytes = content.toByteArray(Charsets.UTF_8)
                runCatching {
                    // "wt"：只用 "w" 時部分服務不會清空舊內容，新內容較短就會留下壞掉的尾巴。
                    requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
                    requireNotNull(resolver.openInputStream(uri)).use { it.readBytes() }
                }.fold(
                    onSuccess = { if (it.contentEquals(bytes)) BackupWriteResult.SUCCESS else BackupWriteResult.FAILED },
                    onFailure = {
                        if (it is FileNotFoundException ||
                            it is SecurityException
                        ) {
                            BackupWriteResult.MISSING
                        } else {
                            BackupWriteResult.FAILED
                        }
                    },
                )
            }

        override suspend fun exists(location: String): Boolean = displayName(location) != null

        override suspend fun displayName(location: String): String? = withContext(Dispatchers.IO) { nameOf(Uri.parse(location)) }

        override suspend fun keep(location: String): Boolean =
            withContext(Dispatchers.IO) {
                runCatching { resolver.takePersistableUriPermission(Uri.parse(location), PERSIST_FLAGS) }.isSuccess
            }

        override suspend fun release(location: String) {
            withContext(Dispatchers.IO) {
                // 授權可能早就被使用者撤銷，釋放失敗不影響任何事。
                runCatching { resolver.releasePersistableUriPermission(Uri.parse(location), PERSIST_FLAGS) }
            }
        }

        // 不同服務回報「檔案不存在」的方式不同（空結果或拋例外），一律視為找不到。
        private fun nameOf(uri: Uri): String? =
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
    }
