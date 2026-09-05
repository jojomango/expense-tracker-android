package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TESTCASES.md T4.2 的純函式部分（解析/驗證/合併）。真正經過 Room DB 的
 * round-trip 測在 `androidTest`（見 `data/BackupPersistenceTest.kt`）。
 */
class BackupTest {
    private fun samplePayload(schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION): BackupPayload =
        BackupPayload(
            schemaVersion = schemaVersion,
            exportedAt = 0,
            wallets = listOf(BackupWallet("w1", "日常", "TWD", "NONE", null, false)),
            transactions =
                listOf(
                    BackupTransaction("t1", "w1", "EXPENSE", 100, "c1", "2026-08-12", null, 0, 0),
                ),
            categories = listOf(BackupCategory("c1", "飲食", "EXPENSE", "🍜", "#C1502E", isDefault = true)),
            settings = BackupSettings(1, "SYSTEM", null),
        )

    @Test
    @DisplayName("ImportMode 有 REPLACE 與 MERGE 兩種取值")
    fun `import mode has two values`() {
        assertEquals(setOf(ImportMode.REPLACE, ImportMode.MERGE), ImportMode.entries.toSet())
    }

    @Test
    @DisplayName("encode/decode round-trips losslessly")
    fun `encode decode round trip`() {
        val payload = samplePayload()
        assertEquals(payload, decodeBackup(encodeBackup(payload)))
    }

    @Test
    @DisplayName("T4.2.2 — schemaVersion 比目前支援的新，拋 BackupSchemaTooNewException")
    fun `T4 2 2`() {
        assertThrows(BackupSchemaTooNewException::class.java) {
            validateBackup(samplePayload(schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION + 1))
        }
    }

    @Test
    @DisplayName("T4.2.3 — 缺少必要欄位的 JSON，解析階段就整批拒絕")
    fun `T4 2 3`() {
        assertThrows(BackupParseException::class.java) {
            decodeBackup("""{"schemaVersion":1}""")
        }
    }

    @Test
    @DisplayName("T4.2.4 — 格式錯誤的 JSON，拋出可讀錯誤，不 crash")
    fun `T4 2 4`() {
        assertThrows(BackupParseException::class.java) { decodeBackup("not json at all") }
        assertThrows(BackupParseException::class.java) { decodeBackup("") }
    }

    @Test
    @DisplayName("T4.2.5 — merge 模式 id 衝突，保留 updatedAt 較新者")
    fun `T4 2 5`() {
        val existing = BackupTransaction("t1", "w1", "EXPENSE", 100, null, "2026-08-12", null, 0, 100)
        val olderIncoming = existing.copy(amount = 999, updatedAt = 50)
        val newerIncoming = existing.copy(amount = 500, updatedAt = 200)

        assertEquals(existing, mergeTransactionsById(listOf(existing), listOf(olderIncoming)).single())
        assertEquals(newerIncoming, mergeTransactionsById(listOf(existing), listOf(newerIncoming)).single())
    }

    @Test
    @DisplayName("T4.2.6 — merge 模式 id 不衝突，兩邊資料皆保留")
    fun `T4 2 6`() {
        val a = BackupTransaction("t1", "w1", "EXPENSE", 100, null, "2026-08-12", null, 0, 0)
        val b = BackupTransaction("t2", "w1", "EXPENSE", 200, null, "2026-08-13", null, 0, 0)
        val merged = mergeTransactionsById(listOf(a), listOf(b))
        assertEquals(setOf("t1", "t2"), merged.map { it.id }.toSet())
    }

    @Test
    @DisplayName("T4.2.8 — 交易引用不存在的 walletId，拋 BackupIntegrityException")
    fun `T4 2 8 wallet`() {
        assertThrows(BackupIntegrityException::class.java) {
            validateBackup(samplePayload().copy(wallets = emptyList()))
        }
    }

    @Test
    @DisplayName("T4.2.8 — 交易引用不存在的 categoryId，拋 BackupIntegrityException")
    fun `T4 2 8 category`() {
        val payload = samplePayload()
        val withBadCategory = payload.copy(transactions = payload.transactions.map { it.copy(categoryId = "missing") })
        assertThrows(BackupIntegrityException::class.java) { validateBackup(withBadCategory) }
    }

    @Test
    @DisplayName("合法 payload 通過 validateBackup 不拋錯")
    fun `valid payload passes validation`() {
        validateBackup(samplePayload())
    }
}
