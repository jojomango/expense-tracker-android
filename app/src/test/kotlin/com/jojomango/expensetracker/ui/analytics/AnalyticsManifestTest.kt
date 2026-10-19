package com.jojomango.expensetracker.ui.analytics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * SPEC.md §3.7「預設關閉」與 P3 第 3 點「不收集廣告 ID、不做歸因」靠的是 manifest，不是 Kotlin 程式碼：
 * 這幾行被誤刪的話，App 照樣跑得好好的、測試照樣綠，只是首次啟動就開始送資料。所以直接解析檔案。
 */
class AnalyticsManifestTest {
    private val android = "http://schemas.android.com/apk/res/android"
    private val tools = "http://schemas.android.com/tools"
    private val doc =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))

    private fun elements(tag: String): List<Element> =
        doc.getElementsByTagName(tag).let { list ->
            (0 until list.length).map {
                list.item(it) as Element
            }
        }

    @Test
    @DisplayName("T9.1.1 — 分析與當機回報在 manifest 預設關閉，自動畫面回報與廣告相關收集也關閉")
    fun `collection disabled by default`() {
        val metaData = elements("meta-data").associate { it.getAttributeNS(android, "name") to it.getAttributeNS(android, "value") }
        for (
        key in
        listOf(
            "firebase_analytics_collection_enabled",
            "firebase_crashlytics_collection_enabled",
            "google_analytics_automatic_screen_reporting_enabled",
            "google_analytics_adid_collection_enabled",
            "google_analytics_ssaid_collection_enabled",
            "google_analytics_default_allow_ad_personalization_signals",
            "google_analytics_default_allow_ad_storage",
            "google_analytics_default_allow_ad_user_data",
        )
        ) {
            assertEquals("false", metaData[key], "$key 應該是 false")
        }
    }

    @Test
    @DisplayName("SPEC.md P3 — 移除 Firebase 帶進來的廣告 ID 與歸因權限")
    fun `ad id and attribution permissions removed`() {
        val removed =
            elements("uses-permission")
                .filter { it.getAttributeNS(tools, "node") == "remove" }
                .mapTo(mutableSetOf()) { it.getAttributeNS(android, "name") }
        val required =
            setOf(
                "com.google.android.gms.permission.AD_ID",
                "android.permission.ACCESS_ADSERVICES_AD_ID",
                "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
                "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            )
        assertTrue(removed.containsAll(required), "沒有移除：${required - removed}")
    }
}
