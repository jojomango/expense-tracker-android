package com.jojomango.expensetracker.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * UI-SPEC.md §2.3 具名字級。Material 3 的 `Typography` 欄位是固定語意命名，
 * 這裡另外開一個 `AppTypography`，透過 [LocalAppTypography] 往下傳，
 * 用法：`LocalAppTypography.current.balance`。
 */
data class AppTypography(
    val balance: TextStyle = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em),
    val amountInput: TextStyle = TextStyle(fontSize = 54.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.035).em),
    val titleLarge: TextStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
    val donutTotal: TextStyle = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em),
    val navTitle: TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    val rowAmount: TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
    val rowTitle: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    val cardTitle: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    val bodyMedium: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    val caption: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    val tabLabel: TextStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal),
)

val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

/** 金額一律用等寬數字，避免跳動——UI-SPEC.md §1.5。 */
fun TextStyle.tabularNums(): TextStyle = copy(fontFeatureSettings = "tnum")
