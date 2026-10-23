package com.jojomango.expensetracker.ui.i18n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import com.jojomango.expensetracker.domain.AppLanguage
import com.jojomango.expensetracker.domain.LanguagePreference
import com.jojomango.expensetracker.domain.resolveAppLanguage
import java.util.Locale

/**
 * SPEC.md §3.9、D14：不加套件的 App 內語言切換。
 *
 * - Android 13+：使用者的選擇存在系統的「應用程式語言」（[LocaleManager]），跟系統設定頁同步，
 *   改了之後系統自己會重建 Activity。
 * - Android 8–12：存在 SharedPreferences（`app_language.xml`，排除在 Auto Backup 之外——語言是裝置層級的偏好），
 *   改了之後由我們重建 Activity。
 *
 * 兩種情況都在 [wrap] 把 Context 的語言設成 [resolveAppLanguage] 的結果——就算使用者選「跟隨系統」，
 * 也不能交給 Android 自己挑資源：簡體中文、港澳的系統語言要顯示繁中（D13），Android 的資源比對做不到這件事。
 */
object AppLocale {
    private const val PREFS_NAME = "app_language"
    private const val KEY_PREFERENCE = "preference"

    fun preference(context: Context): LanguagePreference =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            when {
                locales.isEmpty -> LanguagePreference.SYSTEM
                resolveAppLanguage(LanguagePreference.SYSTEM, listOf(locales[0].toLanguageTag())) == AppLanguage.ZH_TW ->
                    LanguagePreference.ZH_TW
                else -> LanguagePreference.EN
            }
        } else {
            LanguagePreference.fromStorage(prefs(context).getString(KEY_PREFERENCE, null))
        }

    /** 已經是這個選擇就什麼都不做，避免無謂地重建畫面（E2E 啟動參數每次都會呼叫）。 */
    fun setPreference(
        activity: Activity,
        preference: LanguagePreference,
    ) {
        if (preference == preference(activity)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                when (preference) {
                    LanguagePreference.SYSTEM -> LocaleList.getEmptyLocaleList()
                    else -> LocaleList.forLanguageTags(preference.storageValue)
                }
        } else {
            prefs(activity).edit(commit = true) { putString(KEY_PREFERENCE, preference.storageValue) }
            activity.recreate()
        }
    }

    fun appLanguage(context: Context): AppLanguage = resolveAppLanguage(preference(context), systemLanguageTags(context))

    /** 給 `Activity.attachBaseContext` 與不在 Activity 裡的地方（通知）用。 */
    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(appLanguage(base).tag)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    /** 系統本身的語言清單——不是被 App 語言覆蓋過的那份。 */
    private fun systemLanguageTags(context: Context): List<String> {
        val locales =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java).systemLocales
            } else {
                Resources.getSystem().configuration.locales
            }
        return (0 until locales.size()).map { locales[it].toLanguageTag() }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
