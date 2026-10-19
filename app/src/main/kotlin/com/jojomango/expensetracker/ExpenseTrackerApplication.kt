package com.jojomango.expensetracker

import android.app.Application
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApplication : Application() {
    @Inject lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate() {
        super.onCreate()
        // Firebase 會記住上次執行期設定的開關；萬一跟本機存的同意不一致（例如使用者清除了
        // 這個 App 的部分資料），以使用者的選擇為準。
        analyticsTracker.syncCollectionState()
    }
}
