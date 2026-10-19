package com.jojomango.expensetracker.ui.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase 的參數值只收字串與數字，布林會被默默丟掉；轉成字串在 GA4 報表裡也比 0／1 好讀。
 * [AnalyticsEvent] 的參數值只會是字串或布林，型別在那裡就已經擋住了金額之類的東西。
 */
fun toFirebaseParams(params: Map<String, Any>): Map<String, String> = params.mapValues { (_, value) -> value.toString() }

/**
 * 沒有 `google-services.json` 的組建（CI、剛 clone 下來的 repo）Firebase 不會初始化，
 * 這時呼叫 `FirebaseCrashlytics.getInstance()` 會直接丟例外，所以整個 sink 退化成 no-op。
 *
 * 預設關閉靠的是 AndroidManifest 的 meta-data（App 一啟動、還沒跑到任何 Kotlin 程式碼之前
 * SDK 就已經是關閉的）；這裡只負責在使用者切換時打開或關上。
 */
@Singleton
class FirebaseAnalyticsSink
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AnalyticsSink {
        private val firebaseReady: Boolean by lazy { FirebaseApp.getApps(context).isNotEmpty() }
        private val analytics by lazy { FirebaseAnalytics.getInstance(context) }
        private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

        override fun log(
            name: String,
            params: Map<String, Any>,
        ) {
            if (!firebaseReady) return
            val bundle = Bundle()
            toFirebaseParams(params).forEach { (key, value) -> bundle.putString(key, value) }
            analytics.logEvent(name, bundle)
        }

        override fun setCollectionEnabled(enabled: Boolean) {
            if (!firebaseReady) return
            analytics.setAnalyticsCollectionEnabled(enabled)
            crashlytics.isCrashlyticsCollectionEnabled = enabled
            if (!enabled) {
                // 關閉 = 停止收集，也把還沒送出的東西丟掉：使用者關掉之後不該再有任何資料離開手機。
                crashlytics.deleteUnsentReports()
                // 清掉本機的 app instance id，之後重新開啟會是一個全新的匿名 id，跟關閉前的紀錄接不起來。
                analytics.resetAnalyticsData()
            }
        }
    }
