# TASKS.md — Phase 狀態機（Android 版）

> **這個檔案是 agent 的記憶體。**
> Context window 不會跨 session 保留，但 git 會。所有進度狀態都存在這裡。
>
> 標記說明：`✅ DONE` 已完成並合併 ／ `**NEXT**` 下一個要做的 ／ `⬜ TODO` 尚未開始 ／ `🚧 WIP` 有未合併的 PR

這份文件的 phase 切法對應網頁版 `expense-tracker` 的開發順序（Domain → Persistence →
基礎 UI → 預算 → 分類統計 → 打磨），但**內容不是照抄**——網頁版是「先做陽春 UI、
Phase 8 才回頭做介面改版」的兩階段做法，Android 版從 Phase 0 開始就有完整的
`UI-SPEC.md`，所以每個 UI phase 直接做到位，不用像網頁版一樣分兩輪。

---

## 進度總覽

| Phase | 名稱 | 狀態 | PR |
|---|---|---|---|
| 0 | 地基 | ✅ DONE | (見下方交接筆記) |
| 1 | Domain：金額與時間 | ✅ DONE | (見下方交接筆記) |
| 2 | Domain：實體與預算計算 | ✅ DONE | (見下方交接筆記) |
| 3 | 持久層與匯出匯入 | ✅ DONE | (見下方交接筆記) |
| 4 | 基礎 UI：錢包與交易 CRUD | ✅ DONE | (見下方交接筆記) |
| 5 | 預算與即時餘額 | ✅ DONE | (見下方交接筆記) |
| 6 | 分類與統計 | ✅ DONE | (見下方交接筆記) |
| 7 | 打磨 | **NEXT** | |

---

## 已知技術債

> 跟 phase 進度無關的、可追蹤的優化項目——刻意選擇「現在不做」而不是「忘記
> 做」，跟某個 phase 交接筆記裡順帶提到的坑不同，這裡是專門集中列的清單，
> 之後任何一個 phase 開場讀 `TASKS.md` 都應該掃一眼這裡，看有沒有順手能做的。

| # | 項目 | 狀態 | 觸發條件 |
|---|---|---|---|
| TD-1 | Persistence 測試（`data/RoomTestDb.kt`／`PersistenceTest.kt`／`BackupPersistenceTest.kt`）用 JUnit4（透過 Robolectric）+ `junit-vintage-engine` 橋接，跟 domain 層的 JUnit5 不一致。**不是 bug、不違反 SPEC.md §5**（那條 JUnit5 規定的範圍明文限定在 domain 層），純粹是工具選擇的殘留差異，見 Phase 3 交接筆記完整脈絡 | 擱置，不主動處理 | Robolectric 官方釋出 JUnit5 支援（目前只有一個 0.1.0 的社群 extension，未達生產可用門檻，見 [robolectric/robolectric#3477](https://github.com/robolectric/robolectric/issues/3477)）才評估遷移，沒有的話不用管 |
| TD-2 | Maestro flow 裡刪除列表項目的滑動手勢（`SwipeToDismissBox`）用明確座標百分比（例如 `88%,67%`）而不是錨定元素，因為 `swipe: { direction: LEFT, from: { text: X } }` 常常跨不過 dismiss 門檻。已在 Phase 4（E2E-2）、Phase 5、Phase 6（E2E-8 刪飲食、E2E-9 刪咖啡）踩過同一件事，共 5 次。座標寫死在畫面目前的版面高度上，之後任何 phase 改動上方版面（預算卡、分類清單頭部等）都可能讓座標失準，需要重新校準 | 擱置，不主動處理（座標校準成本目前還算低，重新校準只要跑一次失敗、看截圖、調整 Y% 即可） | 之後有餘裕時，把可滑動刪除的列表項目都加上穩定的 `testTag`，Maestro 改用 tag 定位，一勞永逸解決這個脆弱點；或是這個座標校準成本明顯升高（例如又發生一次「改版面高度、忘記重新校準」的 CI 失敗）時，優先處理 |

---

## Phase 0 — 地基 ✅ DONE

**這個 phase 建議人類主導或至少緊盯著做**，因為它決定了之後每個 phase 能不能順利跑。
如果要交給 agent，務必先確認下面每一項的驗收條件都真的可以自動化檢查。

**目標：** 一個空白 Compose 畫面能組出 debug APK、能在 emulator 上安裝開啟，
CI 全綠，`domain` 零依賴的檢查腳本已經就位。

### 要做的事

1. **建立 Android 專案骨架**
   - Gradle（Kotlin DSL，`build.gradle.kts`），`minSdk = 26`，`targetSdk` 用當前
     最新穩定版
   - Jetpack Compose + Material 3
   - Hilt（`@HiltAndroidApp` 的 Application class）
   - 套件命名建議：`com.<yourname>.expensetracker`（換成你自己的網域反寫）

2. **套件分層骨架**（對應 `CLAUDE.md` 的目錄職責）
   - 建一個空的 `domain` package/module，一個空的 `data`、`ui`、`di`
   - 先放一兩個佔位檔案確保四層都能互相 import 成功（`ui` 依賴 `domain`，
     `data` 依賴 `domain`，`domain` 不依賴任何人）

3. **domain 零依賴檢查腳本**
   - 比照網頁版 `scripts/check-domain-purity.mjs` 的精神：一個純文字掃描，
     檢查 `domain/` 底下所有 `.kt` 檔案的 import 陳述式，出現
     `import android.` 或 `import androidx.`（`kotlin.`／`kotlinx.` 除外）
     就印出違規檔案並以非 0 結束碼結束
   - 可以用 Python、shell script、或一個 Gradle custom task 寫，語言不拘，
     重點是**簡單、不會誤判、CI 第一步就跑**
   - 這是 `CLAUDE.md` 禁令 2 唯一的自動化防線，務必在 Phase 0 就做好，
     不要拖到後面 phase 才補

4. **測試工具鏈**
   - JUnit 5（`testImplementation`）
   - Compose UI Testing（`androidTestImplementation`，`createComposeRule`）
   - Maestro CLI（不是 Gradle 依賴，是一個獨立安裝的 CLI 工具，`.maestro/`
     目錄放 YAML flow 檔）
   - JaCoCo，設定 `domain` module／package 的覆蓋率門檻 90%

5. **靜態分析**
   - ktlint（格式）
   - detekt（程式碼異味檢查，可以先用預設規則集，之後視需要客製化）

6. **`./gradlew verify` 聚合 task**：串起 domain 純淨度檢查 → ktlint → detekt →
   單元測試 + 覆蓋率門檻 → `assembleDebug`

7. **GitHub Actions CI**（`.github/workflows/ci.yml`）
   - `verify` job：跑 `./gradlew verify`
   - （選配，emulator 較慢，可以先只在 `main` 分支跑或用 workflow_dispatch
     手動觸發）`e2e` job：`reactivecircus/android-emulator-runner` 起 emulator，
     跑 `maestro test .maestric/`
   - branch protection：`main` 需要 PR + CI 綠燈才能合併

8. **文件**
   - 把這個 bundle 的 `CLAUDE.md`／`SPEC.md`／`TESTCASES.md`／`UI-SPEC.md`
     放進 repo 根目錄
   - 這份 `TASKS.md` 也放進去，Phase 0 做完後更新成「Phase 0 DONE，Phase 1 NEXT」

### 驗收

- [x] `./gradlew verify` 全綠（本機用 JDK 17 + Gradle 8.9 實際跑過，非只看 agent 自報）
- [ ] `./gradlew assembleDebug` 產出的 APK 能在 emulator／實機安裝並開啟，
      看到一個空白畫面（不用有任何功能，Compose 渲染出來就算過）——
      **這一項本機沒能驗證，見下方交接筆記，需要人類補測**
- [x] domain 純淨度檢查腳本：故意在 `domain/` 底下塞一個
      `import android.content.Context` 進去測試，確認腳本真的會抓到並讓
      CI 失敗（exit code 1），測完把這行刪掉
- [ ] CI 在一個測試 PR 上跑過一次，全綠（draft PR 開出後由 GitHub Actions 驗證）

### 刻意不做

任何業務邏輯、任何真正的畫面內容。這個 phase 純粹是骨架。

### 交接筆記（Phase 0 → Phase 1）

**做了什麼：**
- Gradle Kotlin DSL 骨架：AGP 8.6.1、Kotlin 2.0.21（含獨立的
  `org.jetbrains.kotlin.plugin.compose` — Kotlin 2.0 起 Compose 編譯器外掛
  跟 Kotlin 版本脫鉤，這是新專案容易漏掉的一個外掛）、Compose BOM
  2024.10.01、Hilt 2.52、Room 2.6.1（先加依賴，Phase 3 才會真的用）、
  `kotlinx-datetime`、`kotlinx-serialization`。套件名稱用
  `com.jojomango.expensetracker`（沿用 GitHub 帳號 jojomango，非規格要求，
  可自行改）
- `domain`／`data`／`ui`／`di` 四層各放一個 marker 檔證明 import 方向正確
  （`ui`→`domain`、`data`→`domain`、`di`→`data`），Phase 1 開始寫真正的
  `domain/Money.kt` 等檔案時可以直接刪掉 `DomainMarker.kt`
- `scripts/check-domain-purity.sh`：純 bash + grep 掃描，已手動驗證會抓到
  違規（塞一行 `import android.content.Context` 進去測過，exit 1，測完移除）
- ktlint 1.3.1、detekt 1.23.7（`config/detekt/detekt.yml`，暫時關掉
  `MagicNumber` 規則，Phase 1 `Money`/`Week`/`Month` 落地後應該重新打開，
  屆時很多常數會需要 `const val` 命名或行內註解豁免）
- JaCoCo 90% 覆蓋率的 task（`jacocoTestReport` / `jacocoCoverageVerification`）
  已經寫好但**故意沒有接進 `verify`**——`domain` 現在只有一個空 marker
  檔、沒有測試，接上去每次 build 都會因為 0% 覆蓋率失敗。**Phase 1 的 PR
  必須**在 `app/build.gradle.kts` 加回
  `tasks.named("jacocoTestReport") { finalizedBy("jacocoCoverageVerification") }`
  （原本的程式碼註解裡有寫這行，直接取消註解即可）
- `.github/workflows/ci.yml`：`verify` job（ubuntu runner）+ 選配的 `e2e`
  job（macos runner，`workflow_dispatch` 或 push 到 `main` 才跑，串
  Maestro）。**branch protection 還沒設定**——需要 repo admin 權限透過
  GitHub 網頁或 `gh api` 設定，這次 agent session 沒有處理，人類需要另外
  補上（Settings → Branches → Add rule → main → Require PR + status checks）
- 拿掉了原本規格裡 `de.mannodermaus:android-junit5`（在 instrumented test
  上跑 JUnit5）這個套件——研究後發現版本號不穩定、容易卡建置，且
  Phase 0 用不到（domain 單元測試本來就是純 JVM `testDebugUnitTest`，用
  標準 JUnit 5 就夠；instrumented/Compose UI 測試維持 JUnit4 風格的
  `androidx.test` + `compose-ui-test-junit4`，這是 Compose 測試函式庫目前
  的主流做法，即使專案其他地方用 JUnit5 也一樣）。如果之後真的需要在
  device 上跑 JUnit5，這是刻意的技術選擇偏離，不是漏做

**已知的坑／沒做完的事：**
- ⚠️ **APK 安裝開啟這一項驗收沒有在本機完成**：這台機器是 Apple Silicon
  (M4)，但整條工具鏈（Homebrew、預裝的 JDK）是透過 Rosetta 跑 x86_64，
  導致：(1) 既有的 x86_64 system image 因為 Rosetta 進程拿不到
  Hypervisor.framework 加速，`emulator` 直接 PANIC；(2) 改用原生
  arm64-v8a system image 又發現 SDK 裡的 `emulator` 執行檔本身也是
  x86_64-only，QEMU2 直接拒絕跑 arm64 guest；(3) 嘗試下載 arm64 版 JDK
  讓 `sdkmanager` 抓對應的 arm64 emulator 套件，但下載速度異常慢
  （幾分鐘只有個位數 MB），判斷不值得繼續等。**這是這個 sandbox
  環境的限制，不是專案設定的問題**——`./gradlew assembleDebug` 本身
  成功產出了 APK（`app/build/outputs/apk/debug/app-debug.apk`）。
  **請人類在自己的 Android Studio / 實機上裝一次確認**，這也正好
  對應 `CLAUDE.md`「Phase 0 建議人類主導或至少親自驗收」的提醒
- `local.properties`（指向 `~/Library/Android/sdk`）沒有進 repo（已在
  `.gitignore`），每個開發者需要自己建一份或讓 Android Studio 自動產生
- `.maestro/` 目錄目前只有一個說明用的 README，真正的 flow 從 Phase 4
  才開始寫
- Phase 1 動工前務必看 `SPEC.md` §7 D6（自訂幣別）——決策維持「內建 20
  種 + 使用者自訂代碼」，`domain/Currency.kt` 設計小數位數查詢介面時
  要留擴充點，不要重蹈網頁版拖到最後一個 phase的覆轍

---

## Phase 1 — Domain：金額與時間 ✅ DONE

**必讀：** `SPEC.md` §2（P1/P2）、`TESTCASES.md` T1（Money）、T2（Week）、T5（Month）

**要做的事：**
- `domain/Currency.kt`：幣別代碼與小數位數對照表（至少涵蓋網頁版列的 20 種：
  TWD, JPY, USD, EUR, KRW, CNY, HKD, GBP, AUD, SGD, THB, VND, MYR, PHP, IDR,
  INR, CAD, CHF, NZD, MOP）
- `domain/Money.kt`：整數最小單位（`Long`），建構、解析、格式化、加減、`percentOf`
- `domain/Week.kt`：`weekRangeOf(date, weekStartDay)`、週分組
- `domain/Month.kt`：`monthRangeOf(date)`

**驗收：** T1、T2、T5 全過，domain 覆蓋率 ≥ 90%，UI 仍是空白畫面。

**已知的坑（先想清楚再動工，不要跟網頁版一樣拖到最後才處理）：**
自訂幣別小數位數（`SPEC.md` §7 D6）。網頁版直到 Phase 7 都還沒解決這題，
一路被 UI 擋住沒有爆炸。Android 版建議在這個 phase 設計 `Currency.kt` 時，
就先決定好「使用者自訂幣別代碼」要怎麼取得小數位數（例如：新增時要求輸入
小數位數，或是預設全部當 2 位小數 + 一個「這個幣別沒有小數」的勾選框）。
不強制要求這個 phase 就實作完，但至少在 API 設計上留好擴充點。

### 驗收

- [x] T1（Money）、T2（Week）、T5（Month）全過（54 個測案：Money 21、Week 26、Month 7）
- [x] domain 覆蓋率 96%（門檻 90%），`jacocoCoverageVerification` 已接進 `verify`
- [x] `./gradlew verify` 全綠（domain 純淨度 + ktlint + detekt + 測試 + 覆蓋率 + assembleDebug）
- [x] UI 仍是空白畫面（這個 phase 沒有動 `ui`/`data`/`di`，只加 `domain`）

### 交接筆記（Phase 1 → Phase 2）

**做了什麼：**
- `domain/Currency.kt`：`CurrencyInfo`（code/decimalDigits/symbol）+ 內建 20 種幣別
  （`Currencies.builtIn`）+ `CurrencyRegistry` 類別。**D6 決策落地方式**：
  `CurrencyRegistry(custom: Map<String, CurrencyInfo> = emptyMap())`——domain
  本身不持有任何裝置/資料庫相關的可變狀態，自訂幣別由呼叫端（Phase 3+ 讀取
  使用者設定後）組出 `custom` map 傳進來。`Money.of`/`parse`/`sum` 都接受
  一個預設值為空的 `registry` 參數，預設情況下只認得內建 20 種。
- `domain/Money.kt`：`amount: Long` + `currency: CurrencyInfo`，`internal
  constructor`（外部一律走 `of`/`parse`/`sum` 這幾個 companion 工廠函式，
  確保幣別一定經過 `CurrencyRegistry` 解析過，不會建出「不知道小數位數」的
  Money）。`format()` 是手刻的千分位分組邏輯，**沒有用 `java.text.NumberFormat`**
  ——那個會受 JVM 預設 locale 影響，手刻雖然多寫幾行，但每台裝置行為保證一致。
- `domain/Week.kt`：`Week.rangeOf` 用 `date.dayOfWeek.value`（1=一...7=日）跟
  `weekStartDay.value` 算 offset；`Week.groupByWeek` 是泛型函式（`<T>` +
  `dateOf: (T) -> LocalDate` lambda），因為 Phase 1 還沒有 `Transaction`
  型別（Phase 2 才有），先做成泛型可以直接被 Phase 2 拿去用，不用重寫。
- `domain/Month.kt`：`Month.rangeOf` 用 kotlinx-datetime 的
  `plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)` 算月底，不用手刻
  每個月天數表。
- `domain/DateRange.kt`：`Week`/`Month` 共用的 `data class DateRange(start,
  end)`，帶一個 `operator fun contains`。
- 踩到的坑：kotlinx-datetime 這個版本的 `DayOfWeek` **沒有** `isoDayNumber`
  屬性（那是某些版本才有的），要用 `.value`（1=一...7=日）。如果之後升級
  kotlinx-datetime 版本，這裡可能要重新確認。
- JaCoCo 90% 覆蓋率門檻已經正式接進 `verify`（`app/build.gradle.kts` 底部
  `tasks.named("jacocoTestReport") { finalizedBy("jacocoCoverageVerification") }`
  已取消註解），目前 domain 覆蓋率 96%。
- Phase 0 留下的四個 marker 檔（`DomainMarker`/`DataMarker`/`UiMarker`/
  `DiMarker`）全部刪除——它們的任務（證明 domain/data/ui/di 互相 import 方向
  正確）已經在 Phase 0 的 CI 裡驗證過且合併了，繼續留著只是佔位噪音。
  Phase 2/3 開始寫 `Wallet`/`Transaction`/`Category`、Room、Hilt 綁定時，
  這幾個空 package 目錄會重新長出真正的檔案。

**已知的坑／下一個 phase 要注意：**
- `Money.sum(items, code)` 目前**不驗證 registry 一致性以外的東西**——如果
  `items` 裡混了不同幣別（跟 `code` 不符），會在 fold 裡逐筆 `require` 拋錯，
  但錯誤訊息只會指出第一個不符的那一筆，不會列出全部違規項目，這對 Phase 2
  做 `summarizeByCategory` 之類的彙總函式應該夠用，但如果之後需要更詳細的
  診斷資訊可以再擴充。
- `CurrencyRegistry` 目前是無狀態的（每次呼叫端自己組 `custom` map 傳進來），
  Phase 2 的 `Wallet` 型別如果要儲存「這個錢包用什麼幣別」，儲存的應該是
  幣別代碼字串（`currency: String`），不是 `CurrencyInfo` 物件本身——`Wallet`
  也不該持有 `CurrencyRegistry`，需要金額運算時才在呼叫端組出對應的 registry。
- `Week.groupByWeek` 的泛型設計刻意讓 Phase 2 的 `Transaction` 型別可以直接
  傳 `list of Transaction` + `{ it.date }` 進來，不需要另外包一層。

---

## Phase 2 — Domain：實體與預算計算 ✅ DONE

**必讀：** `SPEC.md` §3.1~§3.4、`TESTCASES.md` T3（Budget，含 T3.7/T3.8）、T6（Category）

**要做的事：**
- `domain/Wallet.kt`／`Transaction.kt`／`Category.kt`：型別與驗證規則
- `domain/Budget.kt`：`calculateWeeklyBalance`、`calculateTotalBalance`、
  `calculateWeeklyExpenseTotal`、`summarizeByCategory`、`summarizeWeeklyTrend`、
  `daysLeftInWeek`、`dailyAllowance`
- 分類刪除規則（`assertCanDeleteCategory` 或等義命名）
- 分類固定色：`Category` 從一開始就有 `color: String` 欄位（`#rrggbb`），
  種子色照 `UI-SPEC.md` §2.2 表格填。**這是 Android 版跟網頁版最大的行為差異**——
  網頁版分類色是 Phase 10 才補上去的（需要 migration），Android 版沒有這個歷史
  包袱，第一天就做對

**驗收：** T3、T6 全過。

### 驗收（實際結果）

- [x] T3（Budget，44 案）、T6（Category，10 案）全過
- [x] domain 覆蓋率 94%（門檻 90%）
- [x] `./gradlew verify` 全綠，乾淨重跑過一次確認可重現

### 交接筆記（Phase 2 → Phase 3）

**做了什麼：**
- `domain/Wallet.kt`：`BudgetMode` enum + `Wallet` data class。`budgetAmount`
  的合法性跟 `budgetMode` 綁在一起檢查（`NONE` 時必須是 `null`；
  `WEEKLY`/`TOTAL` 時必須 `>= 0`——**注意是 `>= 0` 不是 `> 0`**，因為
  T3.2.6 明確測了「總預算為 0」這個邊界情境，一開始寫成 `> 0` 會擋掉這個
  合法測案，動工前沒想到，跑測試時才發現，Phase 3 如果要加其他數值欄位
  的驗證，記得先對一遍 TESTCASES.md 的邊界值再決定要不要用嚴格不等式）
- `domain/Transaction.kt`：`amount` 一律正整數（`type` 決定方向），`date`
  是 `LocalDate`（不含時間），`createdAt`/`updatedAt` 是 `kotlinx.datetime.
  Instant`——**由呼叫端注入，domain 不會自己 new 一個「現在」出來**
- `domain/Category.kt`：`color` 驗證只接受 `#rrggbb` 六位（正規表達式），
  `DefaultCategories.seedDefaults()` 產生 11 個預設分類，色票跟
  `UI-SPEC.md` §2.2 逐字核對過。`colorOf(categoryId, categories)` 是顏色
  查詢的唯一入口——**顏色永遠現查、不快取、不由排序結果決定**（T6.3）
- `domain/Budget.kt`：`calculateWeeklyExpenseTotal`／`calculateWeeklyBalance`／
  `calculateTotalBalance`／`summarizeByCategory`／`summarizeWeeklyTrend`／
  `daysLeftInWeek`／`dailyAllowance` 七個函式全部是 `Budget` object 底下的
  純函式，共用一個 private 的 `sumExpenses` helper 做「篩錢包＋篩支出＋篩
  日期條件」

**設計上比較值得記錄的兩個決定：**
1. **`CategorySummary` 刻意不含 `color` 欄位**（TESTCASES.md T6.3.2 明講
   「summarizeByCategory 的回傳不含任何顏色資訊」）。UI 層要顯示分類色時，
   自己拿 `categoryId` 去呼叫 `colorOf(categoryId, categories)`，不要指望
   從彙總結果裡拿顏色。Phase 6 做統計頁圓餅圖/長條圖時要記得這個介面設計。
2. **`summarizeByCategory` 對「categoryId 指向一個已經不在 categories 清單
   裡的分類」自動當成未分類處理**（不會拋錯、也不會另開一組）。這是為了
   同時滿足 T3.5.3（分類被刪除後交易歸入未分類）又不用在 domain 層額外
   實作「分類刪除時大量更新交易」這個 side-effecty 的操作——那個操作本身
   應該在 Phase 3 的 Repository 層做（刪分類時把相關交易的 `categoryId`
   一併更新為 `null`），`summarizeByCategory` 這裡的防禦性寫法只是確保
   「就算 Repository 那層還沒來得及更新，彙總也不會遺失資料或炸掉」。

**已知的坑／下一個 phase 要注意：**
- `DefaultCategories.seedDefaults()` 用 `java.util.UUID.randomUUID()` 產生
  id（可用 `idGenerator` 參數覆寫，測試都有指定固定 id，不受影響）。
  `java.util.UUID` 是 JVM-only，如果之後真的要做 Kotlin Multiplatform
  抽出 domain module 給 iOS 用，這裡要換成多平台的 UUID 函式庫——現在先
  不處理，純 Android 專案階段沒有這個問題
- Phase 3 寫 Room `@Entity` 時，`Wallet`/`Transaction`/`Category` 這幾個
  domain 型別建議直接對應（欄位名稱幾乎可以照抄），不需要另外設計一套
  「DB 版」型別再轉換——除非之後真的需要 Room 專屬的欄位（例如某些
  index/foreign key 相關的東西無法用純 data class 表達）
- `Budget` 裡所有函式都吃「已經篩選好的 `transactions: List<Transaction>`」
  當參數，本身不管資料是從哪裡來、也不做任何 IO——Phase 3 的 Repository
  查出資料後直接餵給這些函式即可，不需要在 domain 層再包一層查詢邏輯

---

## Phase 3 — 持久層與匯出匯入 ✅ DONE

**必讀：** `SPEC.md` §3.5、§3.6、`TESTCASES.md` T4（Persistence）

**要做的事：**
- Room schema（`@Entity`／`@Dao`），schema version 從 1 開始
- Repository 介面放 `domain`，Room 實作放 `data`
- 備份資料的形狀、JSON 剖析、schema 驗證（含參照完整性）、merge 邏輯——
  這塊邏輯本身跟網頁版 `backup.ts` 的精神完全一樣，可以直接照著翻譯成 Kotlin
  （`kotlinx.serialization` 做 JSON 序列化）
- Storage Access Framework 串接（`ActivityResultContracts.CreateDocument`／
  `OpenDocument`）——這塊碰 Android API，放在 `ui` 或 `data` 都可以，不能放 `domain`

**驗收：** T4 全過（Room in-memory database 測）。

### 驗收（實際結果）

- [x] T4.1（Persistence，7 案）、T4.2（匯出/匯入，9 案）全過，**跑在
      `data/src/test/`（純 JVM，用 Robolectric），符合 TESTCASES.md 原本的
      測試分層**——見下方「已批准的新套件」
- [x] T4.2 的純函式部分（decodeBackup/validateBackup/mergeTransactionsById）
      額外在不需要 Robolectric 的 domain 測試裡跑過一份
- [x] `./gradlew verify` 全綠，domain 覆蓋率 93%，乾淨重跑過一次確認可重現
- [x] 全部 146 個測試（domain + persistence）都在本機**實際跑過並通過**，
      不需要 emulator，也不受這台機器的 Apple Silicon/Rosetta 環境限制

### 已批准的新套件：Robolectric

**過程**（也記錄在 `data/RoomTestDb.kt` 的註解裡）：一開始想用 Room 2.7 的
「bundled SQLite driver」在純 JVM 跑 Room，不需要新套件——實測後卡在真正的
死路：AGP（Android Gradle Plugin）模組的 Gradle variant-aware 依賴解析，
永遠只會抓到 `androidx.sqlite:sqlite-bundled` 的 **Android target**
artifact（裡面包 Android ABI 的 `.so`），不會抓到桌機能載入的 native
library。退而求其次先改用 `androidTest`（不需要新套件，但本機沒有堪用的
emulator，測試沒辦法在本機執行）。

跟人類討論後，確認 **Robolectric 才是業界對這個問題的主流答案**（在 AGP
模組裡不開 emulator、純 JVM 跑 Room/`Context` 相關測試的標準做法），人類
批准新增這個套件。改用 Robolectric 後：
- Persistence／Backup 測試搬回 `data/src/test/`，符合 TESTCASES.md 原本的
  分層
- 全部在本機的 `testDebugUnitTest` 裡實際跑過、通過，不受 emulator 環境
  限制
- Robolectric 只支援 JUnit4（不是 domain 層要求的 JUnit5），用
  `junit-vintage-engine` 讓兩種測試在同一個 Gradle test task 共存——這是
  跟 TESTCASES.md 分層表格唯一還留著的一個小差異（JUnit4 vs JUnit5），
  影響範圍很小（只有 persistence 測試用 JUnit4，domain 測試仍是 JUnit5）

### 交接筆記（Phase 3 → Phase 4）

**做了什麼：**
- Room `@Entity`／`@Dao`：`WalletEntity`／`TransactionEntity`／
  `CategoryEntity`／`SettingsEntity`（單行表，`id` 固定 0）。`TransactionEntity`
  用 `ForeignKey`（`walletId` CASCADE 刪除、`categoryId` SET_NULL）把
  SPEC.md §3.1/§3.3 的刪除規則直接刻進 schema，不用在程式碼裡手動維護
  一致性
- `data/Converters.kt`：`LocalDate` 存 ISO 字串、`Instant` 存 epoch millis
- Repository 介面在 `domain/Repositories.kt`，Room 實作在
  `data/Repositories.kt`（`RoomWalletRepository` 等）。`WalletRepository.
  delete`／`CategoryRepository.delete` 分別包了 `assertCanDeleteWallet`／
  `assertCanDeleteCategory` 這兩個 Phase 2 就寫好的純函式守門
- `domain/Backup.kt`：匯出/匯入的資料形狀（`BackupWallet`／
  `BackupTransaction`／`BackupCategory`／`BackupSettings`／`BackupPayload`，
  都用扁平的原生型別欄位，`LocalDate`/`Instant` 存成 String/Long，避開
  kotlinx-datetime 需要額外 serializers 模組的麻煩）+ `encodeBackup`／
  `decodeBackup`／`validateBackup`／`mergeTransactionsById` 四個純函式，
  全部不碰 Room，可以直接在 `test`（JVM）測
- `data/Repositories.kt` 的 `RoomBackupRepository`：`replace`/`merge` 都包
  在同一個 `db.withTransaction {}` 裡，靠「先驗證、驗證通過才寫入」
  保證原子性（T4.2.3），不需要額外的 DB 回滾邏輯
- `data/Migrations.kt`：目前是純文件骨架（`AppDatabase.version` 還是 1，
  `exportSchema = false`）。**T4.1.5（migration test）沒有寫成真正的測試**
  ——沒有真的 v2 schema 可以測，硬做一個假的 v2 只是為了測而測，選擇誠實
  記錄「這是骨架、真的要加 migration 時照 KDoc 裡的範例走」而不是生出一個
  空洞的測試

**已知的坑／下一個 phase 要注意：**
- Storage Access Framework（`ActivityResultContracts.CreateDocument`／
  `OpenDocument`）**還沒串接**——那需要一個真正的 Activity/Compose 畫面
  去觸發檔案選擇器，Phase 0~3 刻意不碰 `ui/`，所以這塊留到 Phase 4 有畫面
  之後（或 Phase 7 打磨時）再做。`BackupRepository.export()`／`import()`
  已經把「資料要怎麼變成 JSON、JSON 要怎麼驗證寫回 DB」都準備好了，
  Phase 4/7 的 UI 只需要拿 SAF 選出來的 `Uri` 讀/寫 bytes，呼叫這兩個函式
  就好，不需要再碰 domain 或 data 層的邏輯
- Hilt 的 DI 綁定（`@Module`／`@Provides` 提供 `AppDatabase`、把
  `RoomWalletRepository` 等綁定到對應介面）**還沒寫**——Phase 0 建立的
  `di/` package 目前是空的。等 Phase 4 真的有 `ViewModel` 需要注入
  Repository 時再補，現在寫沒有東西可以驗證會不會漏綁
- `Wallet`/`Category` 沒有 `updatedAt` 欄位，所以 merge 匯入時這兩種資料
  「匯入端直接覆蓋同 id 的既有資料」，不像 `Transaction` 有時間戳可比較。
  如果之後想讓 `Wallet`/`Category` 也支援更精細的 merge 衝突解決，要先
  幫它們加 `updatedAt` 欄位（會是一次真正的 schema migration，正好可以
  拿來練 `Migrations.kt` 裡說的那個流程）

---

## Phase 4 — 基礎 UI：錢包與交易 CRUD ✅ DONE

**必讀：** `UI-SPEC.md` §3、§4.1、§4.3、§5、§7

**要做的事：**
- Compose Navigation 路由骨架
- 底部導覽（`NavigationBar` + 中央 FAB）
- 首頁標題區（錢包名稱按鈕 + 設定連結）、錢包切換 `ModalBottomSheet`
- 交易列表（依週分組，滑動刪除）、記帳頁（金額優先 + 自製數字鍵台 + 分類網格）
- 首次啟動引導：建立第一個錢包

這個 phase**直接做 `UI-SPEC.md` 描述的完整互動**（自製數字鍵台、分類網格、
滑動刪除），不是先做一個陽春表單版本——因為 `UI-SPEC.md` 從一開始就存在，
沒有理由先做醜的再改。

**驗收：** E2E-1、E2E-2、T8.1、T8.2 全過（Maestro）。

### 交接筆記（Phase 4 → Phase 5）

**做了什麼：**
- `domain/TransactionInput.kt`：金額輸入位數限制的純函式（`appendDigit`/`deleteDigit`），
  對應 T7.1.1~T7.1.7，記帳頁的自製數字鍵台直接呼叫這兩個函式，UI 本身不含這段邏輯。
- `domain/Week.kt` 新增 `groupTitle()`，把「本週 / 上週 / 3/1–3/7」這種標題字串的判斷
  也留在 domain（純函式、注入 `referenceDate`），對應 T7.2.1~T7.2.4。
- `ui/navigation/Routes.kt` + `MainActivity.kt`：`NavHost` + 底部 `NavigationBar`
  （首頁／統計）+ 中央浮動 FAB（記帳），`ADD_TRANSACTION` 路由不帶 walletId 參數——
  一律用當前選中的錢包，`EDIT_TRANSACTION_PATTERN` 帶 `transactionId`。
- `ui/home/HomeViewModel.kt`：`combine(wallets, categories, settings, selectedWalletId)`
  产生 `HomeContext`（私有中介 data class，避免自我參照的 state 更新寫法），再
  `flatMapLatest` 到目前錢包的交易流；`switchWallet()` 同時把選擇寫回
  `Settings.defaultWalletId`，下次啟動記得住上次選的錢包。
- `ui/home/HomeScreen.kt`：標題列、預算卡（先用 domain 現成的 `Budget` 計算結果，
  Phase 5 會補完整的卡片樣式）、週分組交易列表（`SwipeToDismissBox` 滑動刪除 +
  Snackbar 復原）、空狀態、首次啟動引導表單（E2E-1）。
- `ui/transaction/AddEditTransactionScreen.kt` + `ViewModel`：金額優先版面、支出/收入
  切換、分類網格、日期/備註列、自製數字鍵台；新增交易時的預設錢包解析順序是
  `Settings.defaultWalletId` → 第一個未封存錢包。
- `ui/wallet/WalletSwitcherSheet.kt`：`ModalBottomSheet` 錢包清單 + 打勾目前錢包。
- `ui/theme/{Color,Theme,Type}.kt`：`LightColors`/`DarkColors`/`CategoryColors`
  對應 `UI-SPEC.md` §2.1 的確切色碼；`AppTypography`/`AppExtraColors` 透過
  `CompositionLocal` 提供給所有畫面用，不用 `MaterialTheme.typography` 直接改樣式。
- `di/DatabaseModule.kt`、`di/RepositoryModule.kt`：Hilt 組裝層，把 Phase 3 的
  `Room*Repository` 綁進 domain 定義的介面；這五個類別因此必須是 public
  （原本 Phase 3 為了保守起見標成 `internal`，Phase 4 拿掉了）。

**踩過的坑：**
- ktlint 的標準規則集不認得 `@Composable`，`FunctionNaming` 會把每個 PascalCase
  的 composable 函式都當成違規；新增了根目錄 `.editorconfig`：
  `ktlint_function_naming_ignore_when_annotated_with = Composable`
  （ktlint 官方文件記載的標準做法，不是自訂 workaround）。detekt 有自己獨立的
  `FunctionNaming`/`LongMethod`/`LongParameterList` 規則，需要在
  `config/detekt/detekt.yml` 分別對這三個規則加上 `ignoreAnnotated: ["Composable"]`
  才會放行——**這兩邊的設定互相獨立，不會共用**，之後如果 ktlint 或 detekt
  版本升級要注意這兩處都要保留。
- `config/detekt/detekt.yml` 開了 `buildUponDefaultConfig = true`，所以任何自訂規則
  的 YAML 路徑（哪個規則屬於 `style`／`naming`／`complexity`哪個 section）必須完全
  對應 detekt 內建預設設定檔的路徑，寫錯 section 名稱會直接讓整個 detekt 執行失敗
  （"Property ... is misspelled or does not exist"），不是規則失效而已。
- `HomeViewModel.kt` 用了 `flatMapLatest`（`kotlinx.coroutines.flow` 的實驗性 API），
  需要在檔案開頭加 `@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)`。
- 一開始為了修掉 Kotlin 可見度錯誤（`Room*Repository` 需要是 public 才能被
  `RepositoryModule.kt` 的 `@Binds` 用）用 `sed` 直接刪掉 `internal class` 前綴，
  結果留下縮排不一致的多行建構子寫法，讓 ktlint 的自動格式化本身直接壞掉
  （不是「有格式問題」，是格式化工具跑到一半就丟例外）。教訓：改可見度這種
  結構性變更，寧可整段用 `Write` 工具重寫檔案，也不要用 `sed` 動類別/建構子這種
  多行結構。
- **真的有一個金額換算 bug**，是在補寫 E2E-2 的 Maestro flow、對照 TESTCASES.md
  逐行核對「輸入 120 應該顯示 NT$120.00」時才發現的：`AddEditTransactionViewModel`
  原本直接把鍵台輸入的數字字串（`"120"`）當成 [Money] 的最小單位使用，
  沒有乘上 `10^decimalDigits`，會存成 `NT$1.20` 而不是 `NT$120.00`——編輯既有
  交易時的反向換算也漏了同一件事。修法是把這個換算抽成 domain 的兩個純函式
  `majorDigitsToMinorUnits`/`minorUnitsToMajorDigits`（`domain/TransactionInput.kt`），
  同時補了單元測試；`ui/home/HomeScreen.kt` 首次啟動引導表單原本也是手刻同一段
  換算邏輯（沒有測試覆蓋），一併換成這兩個函式。**教訓：只跑
  `./gradlew verify` 綠燈不代表功能正確**——`verify` 只confirm 程式碼組得出來、
  已寫的測試通過，這次的 bug 完全不影響編譯或既有測試，是實際核對 E2E 規格文字
  時才抓到的，這也是為什麼 CLAUDE.md 一直強調「誠實回報失敗，遠比假裝成功有價值」、
  且「未跑過 Maestro flow」要老實寫進 PR。

**留給下一個 phase 的資訊：**
- 目前 `HomeScreen.kt` 的預算卡是最陽春版本（只顯示金額與進度條），Phase 5 要
  依照 `UI-SPEC.md` §4.2 補完整規格（已用比例文字、剩餘天數、日均可用、
  超支警示樣式）。
- 週起始日設定畫面還沒做（`SettingsScreen.kt` 目前只是佔位），Phase 5 的範圍
  包含這塊。
- **這個 phase 的 UI 沒有在 emulator/實機上手動點過一輪**——跟 Phase 0~3 一樣，
  本機 Apple Silicon Mac 無法跑 x86_64 emulator 加速（HVF 限制），且 CI 的
  `e2e` job 剛換成 `ubuntu-latest` + KVM（PR #5，尚未 merge/驗證），所以
  E2E-1、E2E-2、T8.1、T8.2 這些 Maestro 驗收目前**只做到「程式碼組得出來、
  domain 邏輯有測試」，沒有真正跑過 Maestro flow**。這件事誠實寫進這個 PR 的
  「需要人類決策」，等 PR #5 merge 且真的跑過一次 CI 之後，之後的 phase 才能
  真的透過 CI 拿到 Maestro 驗證結果。
- `.maestro/` 已經補上 `E2E-1-first-launch.yaml`、`E2E-2-add-transaction.yaml`
  兩支真正的 flow 檔案（對應 TESTCASES.md 的 Given/When/Then 逐行照寫）。
  **但這兩支 flow 本身還沒有被真的執行過一次**——CI 的 `e2e` job 只在
  push 到 `main` 或手動 `workflow_dispatch` 時才會跑，PR 上永遠是 skipped；
  在寫這兩支 flow、逐行對照 `AddEditTransactionViewModel` 的金額邏輯時，
  意外發現並修好了上面那個金額換算 bug，這代表「照著規格寫 Maestro flow」
  這件事本身就有抓 bug 的價值，不是可有可無的附加工作。E2E-3~E2E-10 的
  flow 留給之後對應的 phase（例如 E2E-3 需要 Phase 5 的預算卡才有意義）。
- **`e2e` job 拆成獨立的 `.github/workflows/e2e.yml`**（原本跟 `verify` 一起放在
  `ci.yml`）。原因：兩個 job 共用同一個 workflow 檔案時，只要 workflow 被
  `pull_request` 事件觸發，GitHub Actions 就會把檔案裡所有 job 都攤出來評估，
  `e2e` 就算被自己的 `if:` 擋掉不執行，還是會在 PR 的 checks 清單上留下一行
  永遠顯示 `Skipped` 的項目，容易讓人誤以為「這個 PR 應該要跑 e2e 但沒跑」。
  拆成獨立檔案、`on:` 只留 `push: branches: [main]` 和 `workflow_dispatch`
  （不寫 `pull_request`）之後，這個 job 完全不會出現在 PR 的 checks 裡，
  想手動驗證的話用 `workflow_dispatch` 觸發即可。
- **PR #6 merge 進 main 後，`e2e` job 真的跑了一次，結果證實了兩件事：**
  1. **`ubuntu-latest` + KVM 這個修正是有效的**——emulator 33 秒內開機完成、
     APK 順利裝進去，跟本機/舊 `macos-14` 設定「boot timeout」的狀況完全不同。
     這是 PR #5 那個修正第一次被真的驗證過，不再只是「照著官方文件猜應該可以」。
  2. **`E2E-1`/`E2E-2` 這兩支 Maestro flow 第一次執行就抓到兩個真的問題**（都是
     flow 本身寫得不夠嚴謹，不是 app 的 bug）：
     - 兩支都在 `tapOn: "建立錢包"` 這一步失敗（`Element not found`）——
       首次啟動引導表單用的是真正的系統 `TextField`（不像記帳頁用自製鍵台），
       打完最後一個欄位後鍵盤沒收起來，把畫面下方的「建立錢包」按鈕蓋住。
       修法：最後一次 `inputText` 之後、`tapOn: "建立錢包"` 之前加一行
       `hideKeyboard`。
     - 修好上面那個之後，兩支又都在分類網格找不到「飲食」失敗。**一開始以為
       是 app 的 bug，後來拉 CI 上傳的失敗截圖才看清楚 app 完全正常**：
       `CategoryDao.observeAll()`（`data/Daos.kt`）明確寫了
       `ORDER BY isDefault DESC, name ASC`，分類網格是照名稱字母序排列，
       不是 `DefaultCategories.seedDefaults()` 的插入順序——這是合理的實作
       選擇，`UI-SPEC.md` 沒有規定分類網格一定要照種子表格順序顯示。「飲食」
       照字母序排到 7 個支出分類的最後一個，掉到第二列、被 4 欄網格的可視
       範圍裁掉。`scrollUntilVisible` 對這個畫面完全沒用——它挑的預設觸控點
       落在畫面下方的自製數字鍵台（同樣是 `LazyVerticalGrid`，但內容剛好
       塞得下、滑不動），改用錨定在既有元素（如「交通」）上的
       `swipe: { direction: UP, from: { text: ... } }`，才是真的滑到分類網格。
     - **教訓：Maestro flow 失敗時不要光看程式碼猜，先想辦法把當下畫面截圖
       撈出來看**——把 `e2e.yml` 的 `Run emulator + Maestro flows` 這步改成
       `continue-on-error: true`，跑完不論成敗都用 `actions/upload-artifact`
       把 Maestro 預設寫的 `~/.maestro/tests`（每個失敗步驟的截圖 + view
       hierarchy JSON）上傳成 CI artifact，再用一個獨立 step 依真正的
       `outcome` 讓 job 該失敗就失敗。
     - **本機也建好了 arm64 emulator，之後改 Maestro flow 不用每次都等 CI**
       （CI 一輪 3~5 分鐘，本機一輪不到 2 分鐘，還能用 `adb shell input tap`
       + `maestro hierarchy` 即時互動除錯，這次靠這招才抓到下面兩個真的
       app bug）：
       - Homebrew／JDK 這條線目前 `JAVA_HOME` 指到
         `/tmp/jdk17_extracted/...`（x86_64 build，雖然 Mac 是 Apple
         Silicon）——這個 JDK 本身編譯專案沒問題，但只要拿它去跑
         `sdkmanager`，`sdkmanager` 就會誤判 host 是 x86_64，連帶把
         `emulator` 這個 SDK package 也裝成 x86_64 版本，在 arm64 Mac 上
         完全開不了機（`FATAL | Avd's CPU Architecture 'arm64' is not
         supported by the QEMU2 emulator on x86_64 host`）。另外裝的
         `cmdline-tools;3.0` 版本太舊，太舊到根本不認得 arm64 mac 這個
         host、`sdkmanager --list` 連 `emulator` package 都列不出來。
         解法：另外下載一份 arm64 原生 Temurin 17（暫放
         `/tmp/jdk17_arm64/`）+ 新版 `cmdline-tools;12.0`（暫放
         `/tmp/cmdline-tools-new/`），用這兩個重新跑
         `sdkmanager "emulator"`，才抓到真正的 `emulator-darwin_aarch64`
         套件。**這兩個暫存目錄只在這次 session 的 sandbox 裡，之後要嘛
         正式導進專案的本機開發文件、要嘛每次都要重新設定**——這是留給
         人類的後續：值得裝一份真正的 arm64 JDK 取代 Homebrew 現在用的
         x86_64 版本，一勞永逸解決這整條問題。
       - `Phase0_arm64` 這個 AVD（Android 14, arm64-v8a）系統映像其實早就
         下載好、放在那邊沒用到，Phase 0 交接筆記裡沒寫清楚為什麼。
       - Maestro CLI 需要 Java 17+ 在 PATH 上第一順位（預設系統 `java` 是
         1.8），要用同一份 arm64 JDK。
     - **用本機這條線抓到兩個真的 app bug**（跟上面「flow 寫得不夠嚴謹」
       不同，這兩個是 `ui/transaction/AddEditTransactionScreen.kt` 本身的
       問題）：
       1. `CategoryGrid` 的 `.clickable` 只加在 50dp 的圖示 `Box` 上，
          分類名稱的 `Text`（圖示下方那行字）完全不在可點擊範圍內——使用者
          點在分類文字上會沒有任何反應。用 `adb shell input tap` 對著
          `maestro hierarchy` 回報的文字座標手動點，完全沒有選取效果就是
          鐵證；改點圖示本身才會出現選取邊框。**修法：把 `.clickable`
          從內層的圖示 `Box` 移到外層包住圖示+文字的整個 `Column`**，
          這樣點文字或圖示都算數。
       2. `swipe: { direction: LEFT, from: { text: "飲食" } }` 這個刪除手勢
          的預設拖曳距離跨不過 `SwipeToDismissBox` 的刪除門檻（試過拉長
          `duration` 也沒用，距離本身沒變），改用明確座標橫跨整列寬度
          （`start: 88%,37%` → `end: 9%,37%`）才划得過去——這是 flow
          本身的問題，不是 app 的 bug。
     - **順帶注意到、但沒有修的小問題**：編輯交易金額後刪除，Snackbar
       復原提示文字（`已刪除 {分類} {金額}`）顯示的是編輯前的舊金額
       （「已刪除 飲食 NT$120.00」，正確應該是編輯後的 NT$200.00）——
       金額計算跟畫面顯示的實際交易資料本身都是對的（餘額正確回到
       NT$3,000.00），只有這行提示文字用了舊值。沒有測案要求這行文字的
       精確度，這次的 E2E-2 flow 也沒有斷言它，先記錄下來，之後有空
       再查（大概率是 `HomeScreen.kt` 的 `onDelete` lambda 裡某個閉包
       捕捉到了刪除前一刻的舊 `transaction` 物件）。
     - 這些修正在 [PR #7](https://github.com/jojomango/expense-tracker-android/pull/7)，
       已經在本機 arm64 emulator 上連續兩次全綠（`maestro test .maestro/`
       兩支 flow 都 Passed），`./gradlew verify` 也全綠，**推上 CI 之後
       `workflow_dispatch` 也真的跑出 `success`**（[run
       33949964640](https://github.com/jojomango/expense-tracker-android/actions/runs/33949964640)）——
       `e2e` job 本身、`Upload Maestro debug artifacts`、
       `Fail job if Maestro flows failed` 全部通過，這是本機 + CI 雙重
       確認過的結果，不是只憑本機推論。E2E-1、E2E-2、T8.1、T8.2 這幾個
       Phase 4 的驗收項目到這裡才算真正拿到綠燈證據。

---

## Phase 5 — 預算與即時餘額 ✅ DONE

**必讀：** `SPEC.md` §3.4、`UI-SPEC.md` §4.2

**要做的事：**
- 錢包預算設定（none / weekly / total）
- 首頁預算卡（`UI-SPEC.md` §4.2 完整規格：進度條、已用比例、剩餘天數、日均可用）
- 超支警示樣式
- 週起始日設定畫面

**驗收：** E2E-3、E2E-4、E2E-5 全過。**這時 app 已具備核心價值。**

### 交接筆記（Phase 5 → Phase 6）

**做了什麼：**
- `domain/Budget.kt` 的 `daysLeftInWeek`/`dailyAllowance` 其實 Phase 2 就做好了（連
  T3.7/T3.8 測試都在），`UI-SPEC.md` §4.2 也提前提醒過這件事——這個 phase
  在 domain 層幾乎沒有新工作，全部是 UI 接線。
- `ui/home/BudgetCard.kt`（新檔案，從 `HomeScreen.kt` 分出來，避免那個檔案
  超過 detekt 的 `TooManyFunctions` 門檻）：`UI-SPEC.md` §4.2 五個元素全部
  補齊——標籤、金額（+超支圖示）、進度條、「已用 X/Y · 還有 N 天」、
  分隔線下的「日均可用」。「已用 X/Y·還有N天」跟「日均可用」**只在
  `WEEKLY` 模式顯示**——這兩個概念（`daysLeftInWeek`）是週預算特有的，
  `TOTAL` 模式沒有週期可言；`UI-SPEC.md` 沒有明講這點（沒有寫「TOTAL 模式
  不顯示」），這是我的解讀，寫進「需要人類決策」。`TOTAL` 模式改成顯示
  `SPEC.md` §3.4 明講的「已用百分比」（「已用 4%」這種格式），這兩種模式
  顯示的資訊完全不同，不是同一行文字套兩種數字。
- **`TESTCASES.md` E2E-3 要求超支時「警示色 + 圖示」，但 `UI-SPEC.md` §4.2
  明講「不用 emoji 警示圖示」——這兩份文件字面上互相矛盾。** 解法：用
  Material Icons 的向量圖示（`Icons.Filled.Warning`），不是 emoji 字元——
  這同時滿足兩邊，UI-SPEC 說的「不用 emoji」跟 TESTCASES 說的「+圖示」
  都成立。**人類已確認這個解讀合理，並且要求調查「不用 emoji」這條規則
  是不是為了避免另外畫 SVG 圖示資產——調查結果：不是，也沒有這個顧慮。**
  `Icons.Filled.Warning` 來自 `androidx.compose.material:material-icons-extended`，
  這個套件 **Phase 4 就已經加入**（拿來做底部導覽「統計」頁的
  `Icons.Filled.BarChart`），這次只是多引用同一個套件裡現成的一個符號，
  沒有新增依賴、沒有畫任何 SVG、對 APK 大小的影響可忽略。`UI-SPEC.md`
  §3.1 本來就講「圖示用 Material Icons 就好」（底部導覽），emoji 則是用在
  分類圖示、空狀態插圖這種「裝飾性內容」——「已超支不用 emoji」讀起來是
  「這是狀態指示，跟導覽圖示一樣走 Material Icons 路線，不要跟分類 emoji
  那種裝飾性內容混在一起」，不是「完全不准用圖示」。
  **記錄成慣例，供之後任何 phase 需要圖示時參考：需要圖示表達狀態/功能
  （不是裝飾性內容）時，優先用 `material-icons-extended` 裡現成的簡單線條
  圖示（跟現有的 `ArrowDropDown`/`Add`/`Check`/`Delete`/`Home`/`BarChart`
  同一個套件），不用另外畫 SVG 或加新的圖示套件依賴；優先選簡單、好懂、
  線條不複雜的圖示，避免用太花俏或語意不明確的符號。**
- 錢包管理：`ui/wallet/WalletManagementScreen.kt`（列出所有未封存錢包、
  「新增」入口）+ `ui/wallet/WalletEditScreen.kt`/`WalletEditViewModel.kt`
  （新增/編輯共用同一個表單，沿用 `HomeScreen.FirstWalletOnboarding` 的
  欄位風格；幣別建立後不可改，編輯時欄位停用）。`UI-SPEC.md` §7 只說
  「管理錢包…」導向「錢包管理頁」，**沒有給這個頁面的完整視覺規格**——
  這是這個 phase 自己設計、填的空，寫進「需要人類決策」。
- `ui/settings/SettingsScreen.kt` 補上週起始日設定（`FilterChip` 選
  週日～週六，`SettingsViewModel` 寫回 `SettingsRepository`）+ 一個導向
  錢包管理頁的入口。
- `Routes.kt` 新增 `WALLET_MANAGEMENT`/`WALLET_NEW`/`WALLET_EDIT_PATTERN`。

**踩過的坑（都是本機 arm64 emulator 快速反覆驗證抓到的，見下面「本機驗證」）：**
- **一個嚴重的既有 bug（Phase 4 就埋下，Phase 5 才第一次真的觸發）：**
  `MainActivity.ExpenseTrackerApp()` 在 `NavHost` 外面先取一次
  `homeViewModel: HomeViewModel = hiltViewModel()`（給 FAB／錢包切換 sheet
  用），但 `composable(Routes.HOME) { HomeScreen(...) }` 呼叫 `HomeScreen`
  時**沒有把這個實例傳進去**——`HomeScreen` 用自己函式簽章預設參數的
  `hiltViewModel()`，這個預設參數會取得**另一個**、scope 綁在 "home" 這個
  `NavBackStackEntry` 的 `HomeViewModel` 實例，跟 `ExpenseTrackerApp` 那層
  （scope 是整個 Activity）拿到的不是同一個物件。兩個實例各自的
  `selectedWalletId`（`MutableStateFlow`）互不相通，所以在錢包切換 sheet
  點別的錢包，`switchWallet()` 呼叫在其中一個實例上，畫面顯示的是另一個
  實例——**點了完全沒反應，畫面永遠停在原本的錢包**。這在 Phase 4 就存在
  （`WalletSwitcherSheet` 是 Phase 4 做的），但 Phase 4 的 E2E flow 從沒有
  真的測過「切換到另一個錢包」這個動作，一直到 Phase 5 的 E2E-4（多錢包）
  才第一次真的觸發、暴露出來。修法：`HomeScreen(..., viewModel =
  homeViewModel)` 明確共用同一個實例。**教訓：`hiltViewModel()` 當函式
  預設參數這件事本身沒問題，但一旦外層（`NavHost` 外面）也想拿同一個
  ViewModel，一定要顯式傳進去，不能假設「反正都是同一個 Composable 樹，
  應該會拿到同一個」——Navigation Compose 的 ViewModelStoreOwner 是綁在
  NavBackStackEntry 上的，跟外層 Activity scope 是兩回事。**
- `CategoryDao.observeAll()` 的字母序排序（Phase 4 交接筆記提過的同一件
  事）在 Phase 5 的 E2E-4 又踩了一次——新增第二個錢包後要選「飲食」分類，
  一樣需要 `swipe` 先捲到看得到。
- **Maestro flow 本身的技術債**：`.maestro/E2E-2-add-transaction.yaml`
  刪除交易那步用明確座標百分比 `88%,48%`（不是錨定元素），這個座標是
  「交易列表第一列」在**目前這個版面**（週預算模式的完整預算卡，含
  已用/還有N天/日均可用三行）底下的實際位置算出來的——Phase 5 把預算卡
  加高之後，這個座標就跟 Phase 4 時計算的不一樣了，這次真的因為這樣壞過
  一次（`assertNotVisible` 失敗），才重新校準。**這是已知的脆弱點**：
  之後任何一個 phase 只要再改首頁版面高度，這個座標大概率又要重新校準。
  更耐用的做法應該是給交易列表的可滑動容器一個穩定的 `testTag`，用 tag
  定位而不是螢幕座標百分比——這次沒有時間做，留給之後有空的時候。
- **`WalletSwitcherSheet` 目前沒有依 `UI-SPEC.md` §7 顯示「{幣別} ·
  {該錢包當期餘額}」**，只顯示幣別代碼（例如「TWD」），沒有顯示該錢包的
  即時餘額。這是 Phase 4 就有的既有小缺口，Phase 5 沒有處理（E2E-3/4/5
  都不需要這個顯示），需要另外去抓每個錢包各自的餘額才能做，範圍比較大，
  留給之後。同樣地，切換錢包後 `UI-SPEC.md` §7 講的「用 Snackbar 顯示
  『已切換到 {名稱}』」也還沒做。
- **E2E-5（週起始日設定）沒有照抄 `TESTCASES.md` 給的 2026-08-09/08-10
  這兩個固定日期**——這個 emulator 沒有 root，改不了系統時鐘（試過
  `adb shell date -s`、`su 0 date`，都被拒絕：`Operation not permitted`/
  `su: inaccessible`），沒辦法讓「今天」變成任何一個特定日期。改用
  `.maestro/scripts/e2e5-dates.js`（Maestro 的 `runScript` + JS）在執行當下
  即時算出「這週一」跟「這週一的前一天（週日）」，兩者的相對位置關係
  （週一屬本週、週日屬上週）跟 TESTCASES 例子的結構完全一樣，只是不管
  哪一天執行都成立，不用管系統時鐘卡在哪天。**人類已確認這個做法能達到
  預期的測試效果。** 連帶發現：Android 內建 `DatePickerDialog` 的月曆格子
  本身是可以直接用 `tapOn` 點的（每一格的 accessibility text 是完整的
  「Tuesday, September 1, 2026」這種英文字串，跟 app 本身的中文介面無關，
  是 Android 框架自己的 a11y 字串），「換到上個月」的按鈕 content
  description 也是英文的「Change to previous month」——這些都在
  `maestro hierarchy` 裡直接查得到，不用用猜的。

**本機驗證：** 這個 phase 全程用本機 arm64 emulator（`Phase0_arm64` AVD）
反覆迭代——`maestro test .maestro/` 一輪不到 2 分鐘（5 支 flow 全跑約 6
分鐘），比等 CI（每輪 3~5 分鐘、還要等 GitHub Actions 排隊）快很多；配合
`adb shell input tap`/`maestro hierarchy`/`adb shell screencap` 直接互動，
上面兩個真的 bug（HomeViewModel 雙實例、預算卡加高導致座標偏移）都是這樣
抓到的，光看程式碼或猜測座標抓不到。5 支 E2E flow（含 Phase 4 留下的
E2E-1/E2E-2）在本機連續兩輪全綠之後，才推上 CI 用
`workflow_dispatch` 再次確認。

**留給下一個 phase 的資訊：**
- Phase 6（分類與統計）需要的分類管理 CRUD，可以參考這個 phase
  `WalletManagementScreen`/`WalletEditScreen` 的分割方式（列表頁 + 共用的
  新增/編輯表單頁）。
- `WalletSwitcherSheet` 的餘額顯示、切換錢包的 Snackbar，都還沒做，見上面
  「踩過的坑」。
- E2E-2 刪除交易那個 swipe 座標的脆弱點（見上面），如果 Phase 6 又改了
  首頁版面高度，這個座標可能要再校準一次，甚至考慮趁那個 phase 順便
  把它換成 testTag 定位。

---

## Phase 6 — 分類與統計 ✅ DONE

**必讀：** `UI-SPEC.md` §6

**要做的事：**
- 分類管理 CRUD（含分類色選擇，色票用 `UI-SPEC.md` §2.2 的 11 色，不做自由選色）
- 統計頁：本週／本月分類支出佔比（Compose Canvas 手畫圓環）、近 8 週支出趨勢
  （Compose Canvas 手畫長條圖）

**驗收：** E2E-8、E2E-9、E2E-10、T8.3 全過。

### 交接筆記（Phase 6 → Phase 7）

**做了什麼：**
- **T8.3.2（Phase 4/5 遺留的坑，這個 phase 一併補完）**：`WalletSwitcherSheet`
  現在會顯示每個錢包自己的「{幣別} · {該錢包當期餘額}」，不再只顯示幣別代碼。
  加了 `TransactionRepository.observeAll()`（跨錢包的所有交易）+
  `HomeViewModel.walletBalanceTexts: StateFlow<Map<String, String>>`
  （依 walletId 分組、套用跟 `BudgetCard` 一樣的
  `weeklyBalance ?: totalBalance ?: weeklyExpenseTotal` 邏輯）。切換錢包後
  也補上 `UI-SPEC.md` §7 要求的 Snackbar「已切換到 {名稱}」。
- 分類管理：`ui/category/CategoryManagementScreen.kt`（清單 + 滑動刪除，系統
  預設分類刪除會被 `DefaultCategoryException` 擋下並顯示 Snackbar）+
  `CategoryEditScreen.kt`/`CategoryEditViewModel.kt`（新增/編輯共用表單，
  類型建立後不可改，跟 Phase 5 的 `Wallet.currency` immutable 是同一個理由：
  已有的統計/預算計算假設分類 type 不會變動）。色票用 `Color.kt` 既有的
  `CategoryColors` 10 色去重後的 `palette`（`UI-SPEC.md` §2.2 表格有 11 列，
  但「其他（支出）」跟「其他（收入）」共用同一個 `#7A7A80`，去重後剛好
  10 個不同顏色——不是遺漏，是預期行為）。
- 統計頁 `ui/stats/StatsScreen.kt`/`StatsViewModel.kt`：本週／本月分類佔比
  圓環（Compose `Canvas` `drawArc` 手畫，中心疊 Compose `Text` 顯示期間總額，
  沒有引入圖表函式庫）+ 近 8 週支出趨勢長條圖（`Canvas` `drawRect` + 虛線
  平均線，底下另外疊一排等寬 `Box` 當週別標籤，跟 `UI-SPEC.md` §6 說的
  「取巧邏輯」一致）。`StatsViewModel` 刻意直接讀 `Settings.defaultWalletId`
  決定「目前錢包」，不跟 `HomeViewModel` 共用實例——這是為了不要重蹈 Phase 5
  交接筆記裡那個「兩個 NavBackStackEntry-scoped ViewModel 各自狀態不同步」
  的覆轍，因為 `HomeViewModel.switchWallet()` 已經會把選擇立刻寫回
  `Settings.defaultWalletId`，任何畫面直接讀這個 Flow 就能保持同步，不需要
  額外傳實例。

**踩過的坑（本機 arm64 emulator 反覆驗證抓到的）：**
- **又一個「巢狀 `LazyColumn` 放在不可捲動的外層 `Column` 裡」的版面 bug**，
  這次踩了兩次，分屬兩個畫面：
  1. `CategoryManagementScreen.kt` 初稿把「支出」「收入」兩個區段各自包成
     獨立的 `LazyColumn`，兩個都放進同一個不可捲動的外層 `Column`——結果
     畫面上看起來沒事，但完全捲不動，超出螢幕範圍的分類（例如「薪資」
     「獎金」）永遠碰不到，肉眼看截圖才發現。修法：改成單一頂層
     `LazyColumn`，區段標題用 `item {}` 插進 `items(...)` 中間。
  2. `StatsScreen.kt` 的外層也是一個沒有 `verticalScroll` 的 `Column`——這次
     是在跑 E2E-10 驗證「近 8 週支出趨勢」卡片下方的週別標籤時發現的：
     斷言一直抓不到文字，一開始以為是文字內容錯，後來截圖才發現那排標籤
     根本沒進入螢幕可視範圍、而且怎麼滑都滑不動。修法：外層 `Column` 加
     `.verticalScroll(rememberScrollState())`。
  **教訓（寫給下一個 phase，甚至下一次寫任何新畫面時都該想一下）：** 任何
  一頁只要內容「有可能超過一個螢幕高度」，外層容器就必須是
  `LazyColumn`／或明確帶 `verticalScroll` 的 `Column`，**不能只憑肉眼看
  截圖判斷「看起來都有顯示」就通過**——不可捲動的畫面在只有幾筆資料、剛好
  塞得進螢幕的情況下，跟正常畫面長得一模一樣，只有資料多到溢出時才會
  暴露，這正是為什麼這個 bug 連續在兩個不同畫面被埋了兩次都沒在寫程式碼
  當下被發現，而是要跑到 Maestro flow 斷言失敗、看截圖才抓到。
- **`hideKeyboard` 誤觸發返回上一頁**：E2E-8 新增分類「咖啡」那步，表單很短、
  鍵盤根本沒蓋住「建立分類」按鈕，卻加了一行 `hideKeyboard`——結果整個
  flow 被彈回 `CategoryManagementScreen`（不是留在表單上）。原因：這個表單
  沒有真的呼叫出系統鍵盤讓 `hideKeyboard` 去收，Maestro 的 `hideKeyboard`
  底層送出的其實是 BACK 鍵事件，沒有鍵盤可收時，這個 BACK 事件就變成
  單純把畫面導覽退回去。修法：拿掉這行，直接 `tapOn: "建立分類"`。
  **教訓：`hideKeyboard` 不是無副作用的「保險起見加一下」——只有在真的
  需要收鍵盤時才加，加之前先截圖確認鍵盤真的擋住了要點的元素。**
- **`SwipeToDismissBox` 座標脆弱性又中了兩次**（E2E-8 刪「飲食」、E2E-9 刪
  「咖啡」），已經是第 4、5 次踩到同一件事（Phase 4、Phase 5 各一次）。
  已寫進「已知技術債」TD-2，這次沒有花時間做 `testTag` 改造，繼續用
  「每次重新截圖校準 Y%」的做法撐過去。
- **E2E-10 的日期建構方式在特定月曆日子上會整個垮掉，這次真的踩到了**：
  這個 emulator 沒有 root，鎖不住系統時鐘（跟 Phase 5 E2E-5 一樣的限制），
  一開始沿用 E2E-5 的做法，用「這週一」代表本週交易、「上週一」代表
  同月但不同週的交易。但這次跑到「今天剛好落在這個月第一週」（跑測試時
  剛好是 9/5 週六，這週一是 8/31、上週一是 8/24，兩者都在**上個月**）——
  `StatsViewModel` 的 `Month.rangeOf(referenceDate)` 用的是 `referenceDate
  = today()` 自己所在的月份（9 月）去查詢，兩筆「這週」的交易全部被排除
  在外，本月統計整個變成 `NT$0.00`。**這不是 app bug，是相對日期測試在
  特定日子上的真實邊界情況**：只要「今天」在月初第一週，「本週」跟「上週」
  就有很高機率整個落在上個月。修法（`.maestro/scripts/e2e10-dates.js`）：
  - 「本週」交易一律用「今天」本身（保證同週、同月，不用管今天是星期幾）。
  - 「同月但不同週」交易改用「今天 ±7 天」，優先用往前 7 天，若那天跨月
    才改用往後 7 天——任何月份長度都 ≥28 天，數學上至少有一邊一定同月，
    這個保證是通用的，不是碰運氣。
  - 代價：近 8 週趨勢圖的「同月不同週」那筆交易，落點可能是**未來**（例如
    這次是下週），而趨勢圖只回顧過去 8 週（不含未來週），所以那筆交易
    所在週的標籤斷言拿掉了（改成只斷言「本週」的週別標籤，數值本身已由
    domain 的 T3.6 單元測試覆蓋）。**這是相對日期測試在無 root emulator 上
    的已知取捨**，跟 E2E-5 的取捨屬於同一類問題，供之後任何需要用相對
    日期構造「本週/本月/近N週」情境的 flow 參考。

**本機驗證：** 沿用 Phase 4/5 建立的本機 arm64 emulator 快速迭代流程——
`maestro test .maestro/` 全部 8 支 flow 一輪約 11 分鐘（含本次新增的
E2E-10），連續兩輪全綠（8/8 Passed）才推上 CI 用 `workflow_dispatch` 再次
確認。`./gradlew verify`（含 `rm -rf app/build .gradle` 後乾淨重跑一次）全綠。

**留給下一個 phase 的資訊：**
- Phase 7（打磨）如果要動到 `StatsScreen`/`CategoryManagementScreen` 的版面，
  記得兩者外層都已經是可捲動容器了（`LazyColumn`／`verticalScroll`），不用
  再重複踩上面那個坑，但如果新增內容導致高度變化，`.maestro/E2E-8/E2E-9`
  裡刪除分類的 swipe 座標可能又要重新校準（見 TD-2）。
- `SwipeToDismissBox` 座標脆弱性（TD-2）目前累計踩了 5 次，如果 Phase 7
  「打磨」的範圍包含測試基礎設施，值得評估花時間做 `testTag` 改造一次
  解決，而不是繼續每個 phase 各自重新校準。
- `.maestro/scripts/e2e10-dates.js` 的「今天 ±7 天找同月不同週」演算法
  （數學上保證至少一邊成立）是這個 phase 新確立的技巧，之後如果還有
  phase 需要類似的「本週 vs 同月不同週」相對日期情境，可以直接參考、
  複用這個演算法，不用重新推導。

---

## Phase 7 — 打磨

**必讀：** `SPEC.md` §3.6（備份提醒）、`UI-SPEC.md` 全篇（深色模式檢查）

**要做的事：**
- 匯出／匯入 UI 完整串接（Storage Access Framework 的實際錯誤處理：使用者取消
  選檔、檔案不可寫入等情境）
- 備份提醒通知（`WorkManager` 排程，7 天沒備份就發一則本地通知）
- 深色模式檢查（`UI-SPEC.md` 每個色票都要在深色模式下過一遍對比度）
- 空狀態、載入狀態、錯誤處理
- 無障礙檢查：TalkBack 能不能完整操作一輪記帳流程、所有可點區域 ≥ 48dp

**驗收：** 所有 E2E／T8 案例全過，實機（不是只有 emulator）安裝測試通過一輪
「建立錢包 → 記帳 → 看統計 → 匯出備份 → 清除資料 → 匯入還原」完整流程。

---

## 待人類決策的問題

> Agent 發現規格矛盾或需要批准時，寫在這裡，並同時寫進 PR 描述。
> 人類回覆後會把該項移除。

_（目前無）_
