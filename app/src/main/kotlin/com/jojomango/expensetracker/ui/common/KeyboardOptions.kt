package com.jojomango.expensetracker.ui.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

/** 預算金額只收數字：直接給數字鍵盤，也不會進輸入法的組字區。 */
val AmountKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
