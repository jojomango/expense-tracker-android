package com.jojomango.expensetracker.ui.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** TESTCASES.md T10.4.4、T10.4.5 — 介面文字都在 string resources，兩種語言一致（SPEC.md §3.9）。 */
class StringResourcesTest {
    private fun keysIn(folder: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$folder/strings.xml"))

        fun names(tag: String) =
            doc.getElementsByTagName(tag).let { list ->
                (0 until list.length)
                    .map { list.item(it) as Element }
                    .filter { it.getAttribute("translatable") != "false" }
                    .map { "$tag/${it.getAttribute("name")}" }
            }
        return (names("string") + names("plurals")).toSet()
    }

    @Test
    @DisplayName("T10.4.4 — 繁中與英文的 string resources key 集合完全相同")
    fun `T10 4 4`() {
        val en = keysIn("values")
        val zh = keysIn("values-zh-rTW")
        assertTrue(en.size > 100, "英文只有 ${en.size} 個字串，看起來不對")
        assertEquals(emptySet<String>(), en - zh, "繁中漏翻")
        assertEquals(emptySet<String>(), zh - en, "繁中多出英文沒有的 key")
    }

    @Test
    @DisplayName("T10.4.5 — ui 與 domain 的 Kotlin 原始碼沒有寫死的中文介面文字（註解除外）")
    fun `T10 4 5`() {
        val offenders =
            listOf("ui", "domain").flatMap { dir ->
                File("src/main/kotlin/com/jojomango/expensetracker/$dir")
                    .walkTopDown()
                    .filter { it.extension == "kt" }
                    .flatMap { file -> chineseStringLiterals(file.readText()).map { "${file.name}: $it" } }
                    .toList()
            }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    @DisplayName("T10.4.5 — 掃描本身會抓到字串裡的中文，但不會把註解當成違規")
    fun `scanner works`() {
        assertEquals(listOf("\"記帳\""), chineseStringLiterals("val a = \"記帳\" // 註解\n/* 區塊 \"註解\" */\nval b = \"ok\""))
    }
}

private val cjk = Regex("[\\u4e00-\\u9fff]")

/** 去掉 `//` 與 `/* */` 註解後，回傳含中文字的字串常值。夠用的簡化版：不處理 raw string 裡的 `//`。 */
internal fun chineseStringLiterals(source: String): List<String> {
    val withoutBlockComments = source.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
    return withoutBlockComments
        .lines()
        .map { line -> stripLineComment(line) }
        .flatMap { line -> Regex("\"(?:\\\\.|[^\"\\\\])*\"").findAll(line).map { it.value } }
        .filter { cjk.containsMatchIn(it) }
}

private fun stripLineComment(line: String): String {
    var inString = false
    var i = 0
    while (i < line.length - 1) {
        val c = line[i]
        if (c == '\\' && inString) {
            i += 2
            continue
        }
        if (c == '"') inString = !inString
        if (!inString && c == '/' && line[i + 1] == '/') return line.substring(0, i)
        i++
    }
    return line
}
