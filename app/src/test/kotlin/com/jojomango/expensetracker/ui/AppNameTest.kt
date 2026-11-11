package com.jojomango.expensetracker.ui

import androidx.test.core.app.ApplicationProvider
import com.jojomango.expensetracker.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SPEC.md D18 — App 名稱：中文「預算本」、英文「Budget Book」。 */
@RunWith(RobolectricTestRunner::class)
class AppNameTest {
    private fun appName() = ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.app_name)

    @Test
    @Config(qualifiers = "zh-rTW")
    fun `D18 — 中文介面的 App 名稱是預算本`() = assertEquals("預算本", appName())

    @Test
    @Config(qualifiers = "en")
    fun `D18 — 英文介面的 App 名稱是 Budget Book`() = assertEquals("Budget Book", appName())
}
