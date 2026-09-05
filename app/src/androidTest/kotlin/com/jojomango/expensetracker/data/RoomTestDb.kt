package com.jojomango.expensetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/**
 * TESTCASES.md 原本把 persistence 測試放在 `data/src/test/`（純 JVM）。這裡改成
 * `androidTest`——見 TASKS.md Phase 3 交接筆記：Room 2.7 的 bundled SQLite driver
 * 理論上能在純 JVM 跑，但在 AGP（Android Gradle Plugin）模組裡，Gradle 的
 * variant-aware 依賴解析永遠只會抓到 Android target 的 `sqlite-bundled`
 * artifact（裡面包的是 Android ABI 的 `.so`，不是 macOS/Linux 桌機能載入的
 * native library），實測會在 `System.loadLibrary` 那步丟
 * `UnsatisfiedLinkError`。要嘛引入 Robolectric，要嘛走標準 `androidTest`——
 * 兩條路都需要脫離 TESTCASES.md 寫的位置，選了影響較小的後者（不需要新套件，
 * `androidx.test` 已經是 SPEC.md §5 核准的依賴）。
 */
internal fun newInMemoryTestDatabase(): AppDatabase =
    Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
