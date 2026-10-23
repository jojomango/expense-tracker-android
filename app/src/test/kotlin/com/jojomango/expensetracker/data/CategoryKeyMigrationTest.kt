package com.jojomango.expensetracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * TESTCASES.md T10.3.1、T10.3.2 — schema v1 → v2：預設分類補上代號（SPEC.md §3.9）。
 * 用 app/schemas 裡的 1.json 建出真正的舊版資料庫，跑遷移，再確認資料。
 */
@RunWith(RobolectricTestRunner::class)
class CategoryKeyMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private val dbName = "migration-test"

    private fun migratedKeys(vararg rows: String): Map<String, String?> {
        helper.createDatabase(dbName, 1).use { db ->
            rows.forEach { db.execSQL("INSERT INTO categories (id, name, type, icon, color, isDefault) VALUES $it") }
        }
        val db = helper.runMigrationsAndValidate(dbName, 2, true, *Migrations.all)
        return db.query("SELECT id, defaultKey FROM categories").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), if (c.isNull(1)) null else c.getString(1)) }
        }
    }

    @Test
    fun `T10 3 1 — 升級前名稱與內建繁中名稱相同的預設分類補上代號`() {
        val keys =
            migratedKeys(
                "('c1', '飲食', 'EXPENSE', '🍜', '#C1502E', 1)",
                "('c2', '其他', 'EXPENSE', '📦', '#7A7A80', 1)",
                "('c3', '其他', 'INCOME', '📦', '#7A7A80', 1)",
                "('c4', '薪資', 'INCOME', '💰', '#2F8F63', 1)",
            )
        assertEquals("food", keys["c1"])
        assertEquals("other_expense", keys["c2"])
        assertEquals("other_income", keys["c3"])
        assertEquals("salary", keys["c4"])
    }

    @Test
    fun `T10 3 2 — 改過名的預設分類與自訂分類沒有代號，名稱保留`() {
        val keys =
            migratedKeys(
                "('c1', '吃飯', 'EXPENSE', '🍜', '#C1502E', 1)",
                "('c2', '飲食', 'EXPENSE', '🍔', '#123456', 0)",
            )
        assertNull(keys["c1"])
        assertNull("自訂分類剛好也叫「飲食」，仍然不是預設分類", keys["c2"])
        helper.runMigrationsAndValidate(dbName, 2, true).query("SELECT name FROM categories WHERE id = 'c1'").use { c ->
            c.moveToFirst()
            assertEquals("吃飯", c.getString(0))
        }
    }
}
