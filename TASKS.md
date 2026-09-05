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
| 2 | Domain：實體與預算計算 | **NEXT** | |
| 3 | 持久層與匯出匯入 | ⬜ TODO | |
| 4 | 基礎 UI：錢包與交易 CRUD | ⬜ TODO | |
| 5 | 預算與即時餘額 | ⬜ TODO | |
| 6 | 分類與統計 | ⬜ TODO | |
| 7 | 打磨 | ⬜ TODO | |

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

## Phase 2 — Domain：實體與預算計算

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

---

## Phase 3 — 持久層與匯出匯入

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

---

## Phase 4 — 基礎 UI：錢包與交易 CRUD

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

---

## Phase 5 — 預算與即時餘額

**必讀：** `SPEC.md` §3.4、`UI-SPEC.md` §4.2

**要做的事：**
- 錢包預算設定（none / weekly / total）
- 首頁預算卡（`UI-SPEC.md` §4.2 完整規格：進度條、已用比例、剩餘天數、日均可用）
- 超支警示樣式
- 週起始日設定畫面

**驗收：** E2E-3、E2E-4、E2E-5 全過。**這時 app 已具備核心價值。**

---

## Phase 6 — 分類與統計

**必讀：** `UI-SPEC.md` §6

**要做的事：**
- 分類管理 CRUD（含分類色選擇，色票用 `UI-SPEC.md` §2.2 的 11 色，不做自由選色）
- 統計頁：本週／本月分類支出佔比（Compose Canvas 手畫圓環）、近 8 週支出趨勢
  （Compose Canvas 手畫長條圖）

**驗收：** E2E-8、E2E-9、E2E-10、T8.3 全過。

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
