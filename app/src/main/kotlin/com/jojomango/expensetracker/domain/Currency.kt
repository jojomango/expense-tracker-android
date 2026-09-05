package com.jojomango.expensetracker.domain

/**
 * ISO 4217 幣別代碼 + 顯示符號 + 最小單位小數位數。
 */
data class CurrencyInfo(
    val code: String,
    val decimalDigits: Int,
    val symbol: String,
)

/**
 * 內建 20 種常見幣別（SPEC.md §7 D6）。
 */
object Currencies {
    val builtIn: Map<String, CurrencyInfo> =
        listOf(
            CurrencyInfo("TWD", 2, "NT$"),
            CurrencyInfo("JPY", 0, "¥"),
            CurrencyInfo("USD", 2, "$"),
            CurrencyInfo("EUR", 2, "€"),
            CurrencyInfo("KRW", 0, "₩"),
            CurrencyInfo("CNY", 2, "¥"),
            CurrencyInfo("HKD", 2, "HK$"),
            CurrencyInfo("GBP", 2, "£"),
            CurrencyInfo("AUD", 2, "A$"),
            CurrencyInfo("SGD", 2, "S$"),
            CurrencyInfo("THB", 2, "฿"),
            CurrencyInfo("VND", 0, "₫"),
            CurrencyInfo("MYR", 2, "RM"),
            CurrencyInfo("PHP", 2, "₱"),
            CurrencyInfo("IDR", 2, "Rp"),
            CurrencyInfo("INR", 2, "₹"),
            CurrencyInfo("CAD", 2, "C$"),
            CurrencyInfo("CHF", 2, "CHF"),
            CurrencyInfo("NZD", 2, "NZ$"),
            CurrencyInfo("MOP", 2, "MOP$"),
        ).associateBy { it.code }
}

/**
 * 幣別代碼 -> [CurrencyInfo] 的查詢入口。
 *
 * **D6 擴充點：** 使用者自訂幣別（SPEC.md §7 D6）不在 [Currencies.builtIn] 裡，
 * 而是由呼叫端（`data`/`ui` 層，Phase 3+ 讀取使用者設定後）組出 [custom] 傳進來——
 * domain 本身不持有任何跟裝置/資料庫相關的可變狀態。這裡先把「查得到自訂幣別的
 * 小數位數」這個介面留好，至於使用者在 UI 上怎麼輸入自訂幣別的小數位數，
 * 是後面 UI phase 的決定，不影響這裡的介面形狀。
 */
class CurrencyRegistry(
    private val custom: Map<String, CurrencyInfo> = emptyMap(),
) {
    fun resolve(code: String): CurrencyInfo =
        Currencies.builtIn[code]
            ?: custom[code]
            ?: throw IllegalArgumentException("Unknown currency code: $code")
}
