package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T10.1 — 語言決定規則（SPEC.md §3.9）。 */
class LanguageTest {
    private fun resolve(
        preference: LanguagePreference,
        vararg system: String,
    ) = resolveAppLanguage(preference, system.toList())

    @Test
    @DisplayName("T10.1.1 — 跟隨系統，zh-TW → 繁中")
    fun `T10 1 1`() = assertEquals(AppLanguage.ZH_TW, resolve(LanguagePreference.SYSTEM, "zh-TW"))

    @Test
    @DisplayName("T10.1.2 — 跟隨系統，zh-HK → 繁中")
    fun `T10 1 2`() = assertEquals(AppLanguage.ZH_TW, resolve(LanguagePreference.SYSTEM, "zh-HK"))

    @Test
    @DisplayName("T10.1.3 — 跟隨系統，zh-Hans-CN → 繁中（D13）")
    fun `T10 1 3`() = assertEquals(AppLanguage.ZH_TW, resolve(LanguagePreference.SYSTEM, "zh-Hans-CN"))

    @Test
    @DisplayName("T10.1.4 — 跟隨系統，en-US → 英文")
    fun `T10 1 4`() = assertEquals(AppLanguage.EN, resolve(LanguagePreference.SYSTEM, "en-US"))

    @Test
    @DisplayName("T10.1.5 — 跟隨系統，ja-JP → 英文")
    fun `T10 1 5`() = assertEquals(AppLanguage.EN, resolve(LanguagePreference.SYSTEM, "ja-JP"))

    @Test
    @DisplayName("T10.1.6 — 跟隨系統，ja-JP, zh-TW → 英文（只看第一個語言）")
    fun `T10 1 6`() = assertEquals(AppLanguage.EN, resolve(LanguagePreference.SYSTEM, "ja-JP", "zh-TW"))

    @Test
    @DisplayName("T10.1.7 — 跟隨系統，空清單 → 英文")
    fun `T10 1 7`() = assertEquals(AppLanguage.EN, resolve(LanguagePreference.SYSTEM))

    @Test
    @DisplayName("T10.1.8 — 選繁體中文，系統 en-US → 繁中（使用者選擇優先）")
    fun `T10 1 8`() = assertEquals(AppLanguage.ZH_TW, resolve(LanguagePreference.ZH_TW, "en-US"))

    @Test
    @DisplayName("T10.1.9 — 選 English，系統 zh-TW → 英文")
    fun `T10 1 9`() = assertEquals(AppLanguage.EN, resolve(LanguagePreference.EN, "zh-TW"))

    @Test
    @DisplayName("T10.1.10 — 首次安裝的預設值為「跟隨系統」")
    fun `T10 1 10`() = assertEquals(LanguagePreference.SYSTEM, LanguagePreference.DEFAULT)

    @Test
    @DisplayName("語言標籤大小寫與底線寫法（zh_TW、ZH-tw）一樣判斷成中文")
    fun `tag variants`() {
        assertEquals(AppLanguage.ZH_TW, resolve(LanguagePreference.SYSTEM, "zh_TW"))
        assertEquals(AppLanguage.ZH_TW, resolve(LanguagePreference.SYSTEM, "ZH-tw"))
    }

    @Test
    @DisplayName("儲存值可以來回轉換；認不得的值回到預設")
    fun `storage round trip`() {
        LanguagePreference.entries.forEach { assertEquals(it, LanguagePreference.fromStorage(it.storageValue)) }
        assertEquals(LanguagePreference.DEFAULT, LanguagePreference.fromStorage("fr"))
        assertEquals(LanguagePreference.DEFAULT, LanguagePreference.fromStorage(null))
    }
}
