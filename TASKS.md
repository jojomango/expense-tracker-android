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
| 0 | 地基 | **NEXT** | |
| 1 | Domain：金額與時間 | ⬜ TODO | |
| 2 | Domain：實體與預算計算 | ⬜ TODO | |
| 3 | 持久層與匯出匯入 | ⬜ TODO | |
| 4 | 基礎 UI：錢包與交易 CRUD | ⬜ TODO | |
| 5 | 預算與即時餘額 | ⬜ TODO | |
| 6 | 分類與統計 | ⬜ TODO | |
| 7 | 打磨 | ⬜ TODO | |

---

## Phase 0 — 地基 **NEXT**

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

- [ ] `./gradlew verify` 全綠
- [ ] `./gradlew assembleDebug` 產出的 APK 能在 emulator／實機安裝並開啟，
      看到一個空白畫面（不用有任何功能，Compose 渲染出來就算過）
- [ ] domain 純淨度檢查腳本：故意在 `domain/` 底下塞一個
      `import android.content.Context` 進去測試，確認腳本真的會抓到並讓
      CI 失敗，測完把這行刪掉
- [ ] CI 在一個測試 PR 上跑過一次，全綠

### 刻意不做

任何業務邏輯、任何真正的畫面內容。這個 phase 純粹是骨架。

---

## Phase 1 — Domain：金額與時間

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
