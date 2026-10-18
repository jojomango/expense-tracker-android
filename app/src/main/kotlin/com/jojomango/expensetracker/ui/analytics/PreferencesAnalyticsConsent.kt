package com.jojomango.expensetracker.ui.analytics

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 「協助改善 App」開關存在 SharedPreferences（T9.1.7）。這份檔案被排除在 Auto Backup 之外
 * （`res/xml` 的兩份規則檔）：同意是對這台裝置給的，還原到新裝置時應該回到預設的關閉。
 *
 * 讀寫都是同步的：App 啟動時要在任何事件送出之前就知道開關狀態，而這份檔案只有一個布林，
 * SharedPreferences 第一次讀取的成本可以忽略。
 */
@Singleton
class PreferencesAnalyticsConsent
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : AnalyticsConsent {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        override var enabled: Boolean
            get() = prefs.getBoolean(KEY_ENABLED, AnalyticsConsent.DEFAULT)
            set(value) = prefs.edit(commit = true) { putBoolean(KEY_ENABLED, value) }

        private companion object {
            const val PREFS_NAME = "analytics_consent"
            const val KEY_ENABLED = "enabled"
        }
    }
