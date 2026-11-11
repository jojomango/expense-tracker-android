package com.jojomango.expensetracker.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jojomango.expensetracker.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TESTCASES.md T8.4.7～T8.4.11 — 幣別選單的畫面行為（UI-SPEC.md §14.1）。 */
@RunWith(RobolectricTestRunner::class)
class CurrencyPickerFieldTest {
    @get:Rule
    val compose = createComposeRule()

    private var selected = "TWD"

    private fun setField(labelRes: Int = R.string.wallet_currency_label) {
        compose.setContent {
            var code by remember { mutableStateOf(selected) }
            CurrencyPickerField(
                selected = code,
                onSelect = {
                    code = it
                    selected = it
                },
                enabled = true,
                labelRes = labelRes,
            )
        }
    }

    @Test
    @Config(qualifiers = "zh-rTW")
    fun `T8_4_8 — 中文介面選日圓後欄位顯示「日圓 · JPY · ¥」`() {
        setField()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).assertTextContains("新台幣 · TWD · NT$")
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performClick()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performTextInput("j")
        compose.onNodeWithText("日圓 · JPY · ¥").performClick()
        assertEquals("JPY", selected)
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).assertTextContains("日圓 · JPY · ¥")
    }

    @Test
    @Config(qualifiers = "en")
    fun `T8_4_9 — 英文介面輸入「日」仍列出 Japanese Yen`() {
        setField()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performClick()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performTextInput("日")
        compose.onNodeWithText("Japanese Yen · JPY · ¥").performClick()
        assertEquals("JPY", selected)
    }

    @Test
    @Config(qualifiers = "zh-rTW")
    fun `T8_4_7 — 輸入 xyz 只顯示「找不到符合的幣別」`() {
        setField()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performClick()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performTextInput("xyz")
        compose.onNodeWithText("找不到符合的幣別").assertIsNotEnabled()
    }

    @Test
    @Config(qualifiers = "zh-rTW")
    fun `T8_4_10 — 輸入 j 後不選就收起選單，欄位回到目前選取的幣別`() {
        setField()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performClick()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performTextInput("j")
        // 再點一次欄位＝收起選單（ExposedDropdownMenuBox 的切換行為），等同不選就離開
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).assertTextContains("新台幣 · TWD · NT$")
        assertEquals("TWD", selected)
    }

    @Test
    @Config(qualifiers = "zh-rTW")
    fun `T8_4_11 — 編輯既有錢包時幣別欄可以修改，標籤為「預設幣別」`() {
        selected = "JPY"
        setField(labelRes = R.string.wallet_default_currency_label)
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).assertIsEnabled()
        compose.onNodeWithText("預設幣別").assertExists()
        compose.onNodeWithTag(CURRENCY_FIELD_TAG).assertTextContains("日圓 · JPY · ¥")
    }
}
