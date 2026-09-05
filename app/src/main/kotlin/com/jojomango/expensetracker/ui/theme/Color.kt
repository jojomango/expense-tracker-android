package com.jojomango.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

// UI-SPEC.md §2.1 — 顏色 token，light / dark 兩組數值。
object LightColors {
    val background = Color(0xFFF2F1EF)
    val surface = Color(0xFFFFFFFF)
    val onSurface = Color(0xFF111114)
    val onSurfaceVariant = Color(0xFF6B6B70)
    val fg3 = Color(0xFFA3A3A8)
    val sep = Color(0xFFE6E3DF)
    val track = Color(0xFFECE9E6)
    val barBg = Color(0xDCF2F1EF) // rgba(242,241,239,0.86)
    val sheet = Color(0xF5FFFFFF) // rgba(255,255,255,0.96)
    val keypad = Color(0xFFE2DFDA)
    val key = Color(0xFFFFFFFF)
    val accent = Color(0xFFC1502E)
    val danger = Color(0xFFD9463B)
    val income = Color(0xFF2F8F63)
}

object DarkColors {
    val background = Color(0xFF000000)
    val surface = Color(0xFF1C1C1E)
    val onSurface = Color(0xFFFFFFFF)
    val onSurfaceVariant = Color(0xFF98989F)
    val fg3 = Color(0xFF6C6C72)
    val sep = Color(0xFF2C2C2E)
    val track = Color(0xFF2C2C2E)
    val barBg = Color(0xCC0A0A0C) // rgba(10,10,12,0.80)
    val sheet = Color(0xF52C2C2E) // rgba(44,44,46,0.96)
    val keypad = Color(0xFF151517)
    val key = Color(0xFF3A3A3C)
    val accent = Color(0xFFD9673F)
    val danger = Color(0xFFE8564A)
    val income = Color(0xFF3FA878)
}

/** UI-SPEC.md §2.2 分類固定色。 */
object CategoryColors {
    const val FOOD = "#C1502E"
    const val TRANSPORT = "#3F8F6A"
    const val HOUSING = "#A8792F"
    const val SHOPPING = "#2F6F9F"
    const val ENTERTAINMENT = "#8A5FBF"
    const val MEDICAL = "#C04A6E"
    const val MISC = "#7A7A80"
    const val SALARY = "#2F8F63"
    const val BONUS = "#C98B2E"
    const val INVESTMENT = "#4A6FA8"
}
