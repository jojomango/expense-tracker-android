package com.jojomango.expensetracker.data

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * TESTCASES.md T4.3 — Auto Backup 排除規則（SPEC.md §3.6 v2.1、D10）。
 *
 * 直接解析 res/xml 裡的兩份規則檔，而不是只看 manifest 有沒有指到它們：規則檔寫錯
 * （例如路徑多了資料夾、domain 打錯）的話系統不會報錯，只會默默照預設全部備份。
 */
class BackupRulesTest {
    private data class Rule(
        val domain: String,
        val path: String,
    )

    private val deviceState =
        setOf(
            Rule("sharedpref", "backup_reminder.xml"),
            Rule("sharedpref", "notification_permission.xml"),
            // 「協助改善 App」的同意要在每台裝置上重新取得：還原到新裝置時不該默默繼承成開啟。
            Rule("sharedpref", "analytics_consent.xml"),
            // 語言是裝置層級的偏好（SPEC.md §3.9）：換手機時應該跟著新手機的系統語言，不繼承舊手機的選擇。
            Rule("sharedpref", "app_language.xml"),
            Rule("database", "androidx.work.workdb"),
        )

    private fun excludesIn(
        file: String,
        section: String?,
    ): Set<Rule> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/xml/$file"))
        val root =
            if (section == null) {
                doc.documentElement
            } else {
                doc.getElementsByTagName(section).item(0) as? Element
                    ?: error("$file 缺少 <$section> 區段")
            }
        val nodes = root.getElementsByTagName("exclude")
        return (0 until nodes.length)
            .map { i ->
                val e = nodes.item(i) as Element
                Rule(e.getAttribute("domain"), e.getAttribute("path"))
            }.toSet()
    }

    @Test
    @DisplayName("T4.3.1 — Android 12+ 的 data_extraction_rules：雲端備份與裝置間轉移都排除裝置狀態")
    fun `T4 3 1 data extraction rules`() {
        for (section in listOf("cloud-backup", "device-transfer")) {
            val excludes = excludesIn("data_extraction_rules.xml", section)
            assertTrue(excludes.containsAll(deviceState), "<$section> 少排除了 ${deviceState - excludes}")
        }
    }

    @Test
    @DisplayName("T4.3.1 — Android 11 以下的 backup_rules：排除裝置狀態")
    fun `T4 3 1 full backup content`() {
        val excludes = excludesIn("backup_rules.xml", null)
        assertTrue(excludes.containsAll(deviceState), "少排除了 ${deviceState - excludes}")
    }

    @Test
    @DisplayName("T4.3.2 — 記帳資料庫沒有被排除（換機要還原得回來）")
    fun `T4 3 2 ledger database is kept`() {
        val all =
            excludesIn("backup_rules.xml", null) +
                excludesIn("data_extraction_rules.xml", "cloud-backup") +
                excludesIn("data_extraction_rules.xml", "device-transfer")
        assertFalse(all.any { it.domain == "database" && it.path.startsWith("expense-tracker.db") })
        assertFalse(all.any { it.domain == "database" && it.path == "." }, "整個 database domain 被排除了")
    }
}
