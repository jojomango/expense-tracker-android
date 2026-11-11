package com.jojomango.expensetracker.ui.common

import com.jojomango.expensetracker.domain.Currencies
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T8.4.1～T8.4.7、T8.4.9 — 幣別選單的搜尋規則（UI-SPEC.md §14.2）。 */
class CurrencyFilterTest {
    // UI-SPEC.md §14.3 的名稱表；實際 App 從 string resources 讀，這裡直接寫死才能不靠 Android 測規則
    private val names =
        mapOf(
            "TWD" to ("新台幣" to "New Taiwan Dollar"),
            "JPY" to ("日圓" to "Japanese Yen"),
            "USD" to ("美元" to "US Dollar"),
            "EUR" to ("歐元" to "Euro"),
            "KRW" to ("韓元" to "South Korean Won"),
            "CNY" to ("人民幣" to "Chinese Yuan"),
            "HKD" to ("港幣" to "Hong Kong Dollar"),
            "GBP" to ("英鎊" to "British Pound"),
            "AUD" to ("澳幣" to "Australian Dollar"),
            "SGD" to ("新加坡幣" to "Singapore Dollar"),
            "THB" to ("泰銖" to "Thai Baht"),
            "VND" to ("越南盾" to "Vietnamese Dong"),
            "MYR" to ("馬來西亞令吉" to "Malaysian Ringgit"),
            "PHP" to ("菲律賓披索" to "Philippine Peso"),
            "IDR" to ("印尼盾" to "Indonesian Rupiah"),
            "INR" to ("印度盧比" to "Indian Rupee"),
            "CAD" to ("加幣" to "Canadian Dollar"),
            "CHF" to ("瑞士法郎" to "Swiss Franc"),
            "NZD" to ("紐西蘭幣" to "New Zealand Dollar"),
            "MOP" to ("澳門幣" to "Macanese Pataca"),
        )

    private val options =
        Currencies.builtIn.values.map { info ->
            val (zh, en) = names.getValue(info.code)
            CurrencyOption(code = info.code, symbol = info.symbol, nameZh = zh, nameEn = en)
        }

    private fun codes(query: String) = filterCurrencies(options, query).map { it.code }

    @Test
    @DisplayName("T8.4.1 — 查詢字空白：列出 20 種，順序同內建")
    fun `T8 4 1`() {
        assertEquals(Currencies.builtIn.keys.toList(), codes(""))
        assertEquals(20, codes("").size)
    }

    @Test
    @DisplayName("T8.4.2 — 輸入 j：只列出 JPY")
    fun `T8 4 2`() = assertEquals(listOf("JPY"), codes("j"))

    @Test
    @DisplayName("T8.4.3 — 大寫 J 或前後空白：同 T8.4.2")
    fun `T8 4 3`() {
        assertEquals(listOf("JPY"), codes("J"))
        assertEquals(listOf("JPY"), codes(" j "))
    }

    @Test
    @DisplayName("T8.4.4 — 輸入「日」：只列出 JPY")
    fun `T8 4 4`() = assertEquals(listOf("JPY"), codes("日"))

    @Test
    @DisplayName("T8.4.5 — 輸入 peso：只列出 PHP")
    fun `T8 4 5`() = assertEquals(listOf("PHP"), codes("peso"))

    @Test
    @DisplayName("T8.4.6 — 輸入 p：PHP、GBP、MOP（代碼符合的排前面）")
    fun `T8 4 6`() = assertEquals(listOf("PHP", "GBP", "MOP"), codes("p"))

    @Test
    @DisplayName("T8.4.7 — 輸入 xyz：沒有符合的幣別")
    fun `T8 4 7`() = assertEquals(emptyList<String>(), codes("xyz"))

    @Test
    @DisplayName("T8.4.9 — 中文名稱不論介面語言都比對（規則層：比對不看介面語言）")
    fun `T8 4 9`() {
        assertEquals(listOf("JPY"), codes("日"))
        assertEquals(listOf("JPY"), codes("japanese"))
    }
}
