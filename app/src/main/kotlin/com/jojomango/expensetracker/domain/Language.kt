package com.jojomango.expensetracker.domain

/** SPEC.md §3.5 `language` 設定：使用者在設定頁的選擇。 */
enum class LanguagePreference(
    val storageValue: String,
) {
    SYSTEM("system"),
    ZH_TW("zh-TW"),
    EN("en"),
    ;

    companion object {
        /** T10.1.10：首次安裝跟隨系統。 */
        val DEFAULT = SYSTEM

        fun fromStorage(value: String?): LanguagePreference = entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

/** 介面實際使用的語言：只有這兩種（SPEC.md §3.9、D12）。 */
enum class AppLanguage(
    val tag: String,
) {
    ZH_TW("zh-TW"),
    EN("en"),
}

/**
 * SPEC.md §3.9 的決定規則。系統語言清單由外層傳進來（BCP 47 標籤，例如 `zh-Hant-TW`），
 * domain 不碰 `java.util.Locale` 以外的平台 API，也不自己去問系統。
 *
 * 只看**第一個**系統語言：使用者把日文排第一、中文排第二，就是想先看日文；日文我們沒有，就給英文
 * （T10.1.6）。任何中文——不分地區、不分繁簡——都給繁中（D13）。
 */
fun resolveAppLanguage(
    preference: LanguagePreference,
    systemLanguageTags: List<String>,
): AppLanguage =
    when (preference) {
        LanguagePreference.ZH_TW -> AppLanguage.ZH_TW
        LanguagePreference.EN -> AppLanguage.EN
        LanguagePreference.SYSTEM -> {
            val primaryLanguage =
                systemLanguageTags
                    .firstOrNull()
                    ?.substringBefore('-')
                    ?.substringBefore('_')
                    ?.lowercase()
            if (primaryLanguage == "zh") AppLanguage.ZH_TW else AppLanguage.EN
        }
    }
