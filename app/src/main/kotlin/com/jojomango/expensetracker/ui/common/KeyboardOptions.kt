package com.jojomango.expensetracker.ui.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList

/**
 * 幣別代號（ISO 4217）只會是英文字母。App 是繁中時，鍵盤會跟著切到注音，打英文字母還要先切換——
 * 這裡提示鍵盤用英文、自動大寫、不要自動修正（Phase 10 發現：注音鍵盤會把字母留在組字區，根本沒有送出）。
 */
val CurrencyCodeKeyboardOptions =
    KeyboardOptions(
        capitalization = KeyboardCapitalization.Characters,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Ascii,
        hintLocales = LocaleList("en"),
    )

/** 預算金額只收數字：直接給數字鍵盤，也不會進輸入法的組字區。 */
val AmountKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
