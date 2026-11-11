package com.jojomango.expensetracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TESTCASES.md T4.1.10 — schema v3 → v4：交易新增幣別，補為所屬錢包的幣別（SPEC.md §3.6 v2.12）。 */
@RunWith(RobolectricTestRunner::class)
class MultiCurrencyMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun `T4 1 10 — 既有交易全部保留，幣別等於所屬錢包的幣別`() {
        helper.createDatabase("mc-migration", 3).use { db ->
            db.execSQL(
                "INSERT INTO wallets (id, name, currency, budgetMode, budgetAmount, archived) VALUES ('w1', '日常', 'TWD', 'NONE', NULL, 0)",
            )
            db.execSQL(
                "INSERT INTO wallets (id, name, currency, budgetMode, budgetAmount, archived) VALUES ('w2', '東京', 'JPY', 'NONE', NULL, 0)",
            )
            listOf("('t1', 'w1', 10000)", "('t2', 'w2', 800)", "('t3', 'w2', 1200)").forEach { row ->
                val (id, wallet, amount) = row.trim('(', ')').split(", ")
                db.execSQL(
                    "INSERT INTO transactions (id, walletId, type, amount, categoryId, date, note, createdAt, updatedAt) " +
                        "VALUES ($id, $wallet, 'EXPENSE', $amount, NULL, '2026-11-11', NULL, 0, 0)",
                )
            }
        }
        val db = helper.runMigrationsAndValidate("mc-migration", 4, true, *Migrations.all)
        val rows =
            db.query("SELECT id, amount, currency FROM transactions ORDER BY id").use { c ->
                buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getLong(1), c.getString(2))) }
            }
        assertEquals(listOf(Triple("t1", 10000L, "TWD"), Triple("t2", 800L, "JPY"), Triple("t3", 1200L, "JPY")), rows)
    }
}
