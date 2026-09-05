package com.jojomango.expensetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/**
 * TESTCASES.md 原本把 persistence 測試放在 `data/src/test/`（純 JVM）。標準的
 * Room `Context` 需要真的 Android 框架類別，這裡用 Robolectric（見
 * `PersistenceTest.kt`／`BackupPersistenceTest.kt` 的 `@RunWith
 * (RobolectricTestRunner::class)`）在 JVM 裡模擬出來，不需要 emulator。
 *
 * 交接筆記：一開始想用 Room 2.7 的 bundled SQLite driver 避開 Robolectric，
 * 但 AGP 模組的 Gradle variant-aware 依賴解析永遠只會抓到 Android target 的
 * `sqlite-bundled` artifact（Android ABI 的 `.so`，桌機載入不了），實測卡死。
 * Robolectric 才是業界對這個問題的主流答案。
 */
internal fun newInMemoryTestDatabase(): AppDatabase =
    Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
