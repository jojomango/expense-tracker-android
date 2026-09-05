package com.jojomango.expensetracker.domain

private const val MAX_AMOUNT_DIGITS = 8

/**
 * 記帳頁自製數字鍵台的輸入邏輯——UI-SPEC.md §5「最多 8 位數字；前導 0 自動
 * 去除」，測案見 TESTCASES.md T7.1。[input] 是鍵台上的一個按鍵文案
 * （`"0"`.."9"` 或 `"00"`），不是單一字元，因為鍵盤上真的有一顆 `00` 鍵。
 */
fun appendDigit(
    current: String,
    input: String,
): String {
    require(input.isNotEmpty() && input.all { it.isDigit() }) { "input must be digits, was \"$input\"" }
    val base = current.ifEmpty { "0" }
    if (base == "0") {
        return if (input.all { it == '0' }) "0" else input
    }
    val combined = base + input
    return if (combined.length > MAX_AMOUNT_DIGITS) base else combined
}

/** `⌫`：逐字刪除，空字串時維持空字串（TESTCASES.md T7.1.7）。 */
fun deleteDigit(current: String): String = current.dropLast(1)
