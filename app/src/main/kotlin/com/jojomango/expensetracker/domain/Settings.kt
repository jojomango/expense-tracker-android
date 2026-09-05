package com.jojomango.expensetracker.domain

import kotlinx.datetime.DayOfWeek

enum class Theme { LIGHT, DARK, SYSTEM }

/**
 * SPEC.md §3.5 全域設定。`weekStartDay` 用 [DayOfWeek]（跟 [Week]/[Budget] 共用同一個
 * 型別，不用另外維護 0~6 的整數對照表）。變更後所有錢包的週分組即時重算，
 * 不改變任何交易資料——這件事天生成立，因為 [Week]/[Budget] 每次都是即時算，
 * 不快取任何跟 `weekStartDay` 有關的衍生值。
 */
data class Settings(
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val theme: Theme = Theme.SYSTEM,
    val defaultWalletId: String? = null,
)
