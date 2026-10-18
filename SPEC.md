# 記帳本 Android — 專案規格書 v1

> 本文件是專案的唯一真實來源（single source of truth）。
> 所有 agent session 開始前必須先讀本文件與 `TESTCASES.md`。
> 修改本文件 = 修改契約，必須經人工確認。

---

## 0. 這份文件從哪裡來

這是 [expense-tracker](https://github.com/jojomango/expense-tracker)（記帳本 PWA 網頁版）的 **Android 原生重寫版**規格。

§3（功能範圍）與 §4（非目標）跟網頁版**逐條對應、業務規則不變**——網頁版從 Phase 0 做到
Phase 8，已經用超過 250 個測案驗證過這些規則是對的，沒有理由在 Android 版重新發明。
真正改變的只有 §2（架構原則，換了語言與框架的具體約束）、§5（技術棧）、§6（開發階段，
按 Android 的建置順序重新切）。

如果你在做 Android 版時發現某條業務規則講不通，**不代表規格錯了，代表你可能漏看了
網頁版當初為什麼這樣設計**——先去對照網頁版的 `SPEC.md` §7（待確認的設計決策）與
`TASKS.md` 的交接筆記，很可能已經有答案。真的找不到答案，才走「規格矛盾，停下來問人類」
的流程。

---

## 1. 專案目標

一個**純本地、離線可用**的個人記帳 Android App，支援多錢包與多幣別。
不依賴任何雲端帳號或伺服器，**記帳資料完全留在裝置上**。

唯二會連網的功能都是可選的、而且碰不到記帳資料（見 P3）：使用者**主動開啟**的匿名
使用分析與當機回報（§3.7），以及使用者**主動點擊**的贊助（§3.8）。

### 核心價值主張

「我這週還能花多少錢？」— 打開就能立刻看到答案。

---

## 2. 架構原則（不可違反）

這五條是硬約束。Agent 在任何 phase 都不得違反，違反視為該 PR 不合格。

### P1. 業務邏輯與框架完全分離

```
app/src/main/kotlin/<packageid>/
  domain/        ← 純 Kotlin，零 Android／Compose／Room 依賴
    Money.kt
    Currency.kt
    Week.kt
    Month.kt
    Budget.kt
    Wallet.kt
    Transaction.kt
    Category.kt
    Backup.kt
  data/          ← Room 實作，依賴 domain，不被 domain 依賴
  ui/            ← Jetpack Compose，只做渲染與事件轉發
  di/            ← Hilt 組裝層（DI、Repository 綁定）
```

**驗證方式：** CI 內建一個純文字掃描腳本（比照網頁版 `scripts/check-domain-purity.mjs`
的精神）——`domain/` 底下任何檔案出現 `import android.*`、`import androidx.*`
（`kotlin.*`／`kotlinx.*` 除外）即 build 失敗。detekt 也可以另外配一條自訂規則做
第二道防線，但純文字掃描腳本要先有，因為它最簡單、最不會誤判。

理由：這個 domain 層本身也應該具備可攜性——如果之後想做 Wear OS 版、桌面版
（Kotlin Multiplatform Desktop），或是把商業邏輯抽成一個獨立的 KMP module 給
iOS 版共用，都不需要重寫這一層。

### P2. 金額一律以「最小單位整數」儲存

不得使用 `Float` 或 `Double` 表示金額。TWD 100.50 存為 `10050`（分），
JPY 1000 存為 `1000`（圓，0 位小數）。所有運算在整數（`Long`）域完成，
僅在顯示時格式化。

### P3. 記帳資料不離開裝置；網路只用於兩件明確列舉的事

> v2 修訂（Phase 9）：v1 的原文是「App 執行期間不得發出任何網路請求。無後端、無帳號、
> 無遙測、無崩潰回報 SDK」。產品決定加入**預設關閉**的匿名使用分析，以及走 Google Play
> 的贊助，所以把這條從「零網路」改成「記帳資料不離開裝置」。**底線沒有放寬的部分照舊
> 是硬約束。**

1. **記帳資料永遠不得離開裝置。** 錢包名稱、交易金額、分類名稱、備註、幣別、預算金額、
   日期——任何一項都不得出現在上傳的事件、事件參數、使用者屬性、log、當機報告的自訂鍵
   或任何其他網路請求裡。（使用者自己用 SAF 匯出的備份檔、Android 系統的 Auto Backup
   不在此限，那是使用者自己的檔案與雲端空間。）
2. **網路只允許用於兩件事**：
   - 匿名使用分析與當機回報（§3.7）——**預設關閉**，使用者在設定頁明確開啟後才送出任何資料；
     關閉時 SDK 不得送出任何東西（包含 App 首次啟動時）。
   - 贊助（§3.8）——只透過 Google Play Billing。
3. **仍然禁止**：廣告 SDK（任何形式）、廣告 ID、§5 表格以外的任何分析／追蹤／歸因 SDK、
   畫面錄影（session replay）類工具、後端伺服器、帳號、雲端同步。
4. 分析與當機回報的呼叫只能放在 `ui`／`data` 層，`domain` 完全不知道它存在（P1 不變）。

### P4. 每個 phase 結束時專案必須是可執行、可安裝的

不允許「這個 phase 只寫一半、下個 phase 才能跑」。
每個 phase 的 PR 合併後，CI 產出的 debug APK 必須能正常安裝並開啟。

### P5. 測試先於實作

每個 phase 的 PR 必須包含該 phase 的測案，且測案必須先失敗、再由實作轉綠。
Domain 層測試覆蓋率門檻 **90%**（JaCoCo + CI 強制）。

---

## 3. v1 功能範圍

### 3.1 錢包（Wallet）

| 屬性 | 說明 |
|---|---|
| `id` | UUID |
| `name` | 使用者自訂，如「日常」「日本旅遊 2026」 |
| `currency` | ISO 4217 代碼，如 `TWD`、`JPY`。**建立後不可修改** |
| `budgetMode` | `none` \| `weekly` \| `total` |
| `budgetAmount` | 整數最小單位；`budgetMode = none` 時為 `null` |
| `archived` | 布林，封存的錢包不顯示在主畫面 |

**規則：**
- 至少存在一個錢包；不可刪除最後一個錢包
- 錢包之間**不做任何幣別換算**，也**沒有總資產畫面**
- 刪除錢包時，其所有交易一併刪除（需二次確認，並提示交易筆數）
- （v2.1 補充）**不可封存最後一個未封存的錢包**——否則主畫面沒有錢包可以顯示
- （v2.1 補充）封存不影響該錢包的交易資料；封存的錢包可以在錢包管理頁取消封存

### 3.2 交易（Transaction）

| 屬性 | 說明 |
|---|---|
| `id` | UUID |
| `walletId` | 所屬錢包 |
| `type` | `expense` \| `income` |
| `amount` | 正整數，最小單位。方向由 `type` 決定，**amount 永遠為正** |
| `categoryId` | 分類（可為 `null` = 未分類） |
| `date` | 本地曆日（不含時間、不含時區） |
| `note` | 選填，字串 |
| `createdAt` / `updatedAt` | 時間戳，供衝突處理與排序 |

**規則：**
- 金額必須 > 0
- 日期允許未來日期（預先記帳），但預設為今天
- 交易的幣別隱含由錢包決定，不獨立儲存

### 3.3 分類（Category）

| 屬性 | 說明 |
|---|---|
| `id` | UUID |
| `name` | 使用者自訂 |
| `type` | `expense` \| `income` |
| `icon` | emoji 字串（v1 用 emoji，不做圖檔） |
| `isDefault` | 系統預設分類，可改名但不可刪除 |

**規則：**
- 分類**全域共用**，不隸屬於特定錢包
- 刪除有交易的分類時，交易轉移到「未分類」（不連帶刪除交易）
- 首次啟動建立預設分類：
  - 支出：🍜 飲食、🚗 交通、🏠 居住、🛒 購物、🎬 娛樂、💊 醫療、📦 其他
  - 收入：💰 薪資、🎁 獎金、📈 投資、📦 其他

### 3.4 預算與餘額（核心功能）

三種模式：

**`none`** — 不設預算。主畫面不顯示餘額卡片，只顯示本週支出總額。

**`weekly`** — 每週預算（適合日常錢包）
```
本週餘額 = 週預算 − 本週支出總和
```
- 「本週」由使用者設定的週起始日決定
- **只計算 `expense`，不扣除 `income`**（見 §7 決策 D1）
- 每週自動重置，不累積結轉

**`total`** — 總預算（適合旅遊錢包）
```
剩餘總預算 = 總預算 − 該錢包全部支出總和
```
- 不受週期影響，從錢包建立起累計
- 額外顯示：已用百分比

**共通：**
- 餘額可為負（超支），UI 以警示色與圖示呈現
- 餘額顯示必須**即時**：新增/編輯/刪除交易後同一畫面立刻更新，不需重整
  （Compose 的 `StateFlow` 訂閱天然滿足這條，只要 Repository 的寫入操作
  結束後 Flow 會重新 emit）

### 3.5 設定（Settings）

| 設定 | 值 | 預設 |
|---|---|---|
| `weekStartDay` | `0`(週日) ~ `6`(週六) | `1`（週一） |
| `theme` | `light` \| `dark` \| `system` | `system` |
| `defaultWalletId` | 開啟 app 時預設顯示的錢包 | 第一個錢包 |

**`weekStartDay` 是全域設定**，變更後所有錢包的週分組即時重算（不改變任何交易資料）。

### 3.6 資料匯出 / 匯入

**必需功能，非選配。** 這是純本地架構下唯一的備份手段。

- **匯出**：單一 JSON 檔，含 schema version、所有錢包/交易/分類/設定。檔名
  `expense-backup-YYYYMMDD-HHmm.json`。用 Storage Access Framework
  （`ACTION_CREATE_DOCUMENT`）讓使用者選擇存放位置（例如 Google Drive 同步的資料夾），
  對應網頁版「匯出的檔案使用者可以自己存到雲端硬碟」的能力。
- **匯入**：兩種模式
  - `replace`：清空現有資料後匯入（需輸入確認字串）
  - `merge`：以 `id` 為鍵合併，衝突時保留 `updatedAt` 較新者
- 匯入前必須驗證 schema version 與資料完整性，任何一筆不合法則整批拒絕（原子性）
- （v2.1 補充）**Android 系統的 Auto Backup 保留開啟**：記帳資料會由系統備份到使用者自己的
  Google Drive（端對端加密、開發者無法讀取），換機時自動還原。但**裝置狀態要排除在外**——
  備份提醒的時間點、通知權限的詢問紀錄、WorkManager 的排程資料庫——這些屬於「這台裝置」，
  還原到新裝置會讓新裝置繼承舊裝置的計時與排程。
- App 首次啟動後每 7 天提醒一次備份（用 `WorkManager` 排程本地通知，**不是**
  推播——沒有伺服器，純粹是裝置本地排程）

**匯出格式相容性：** 匯出的 JSON schema 盡量跟網頁版一致（欄位名稱、`schemaVersion`
語意相同），方便同一個使用者未來想把網頁版資料手動搬到 Android 版。這不是 v1
的正式功能（不用做「網頁版直接匯入」的 UI 或文件），純粹是設計 schema 時的一個
念頭，讓兩邊不要無謂地產生不相容的格式。

### 3.7 匿名使用分析與當機回報（v2 新增）

目的：知道使用者實際怎麼用、哪裡卡住、哪裡當機，而不是只靠 Play 評論猜。

- 用 Firebase Analytics（GA4 的 App 版）與 Firebase Crashlytics（§5）。
- **預設關閉。** 設定頁有一個開關「協助改善 App」，說明文字寫清楚「只傳送匿名的使用情形
  與當機資訊，不會傳送任何記帳內容」。開關狀態存在本機，使用者隨時可以關閉；關閉後立即
  停止收集與上傳。
- **不設定使用者 ID**、不收集廣告 ID、不開啟任何 Google Signals／廣告個人化相關功能。
- **只允許送出下表列出的事件**，參數也只能是表中列出的值。新增事件要改這份文件。

| 事件 | 參數（只允許這些） | 用途 |
|---|---|---|
| `screen_view` | 畫面名稱（`home`／`stats`／`add_transaction`／`settings`…） | 哪些畫面有人用 |
| `transaction_saved` | `type`（`expense`／`income`）、`is_edit`（布林） | 記帳流程有沒有走完 |
| `wallet_created` | `budget_mode`（`none`／`weekly`／`total`） | 預算模式的選擇分佈 |
| `stats_period_switched` | `period`（`week`／`month`） | 統計頁怎麼被用 |
| `backup_exported` | 無 | 備份功能有沒有人用 |
| `backup_imported` | `mode`（`merge`／`replace`）、`result`（`success`／`failed`） | 還原流程有沒有出錯 |
| `theme_changed` | `theme`（`system`／`light`／`dark`） | 深色模式使用率 |
| `donation_section_opened` | 無 | 贊助入口有沒有人看 |
| `donation_completed` | `tier`（`small`／`medium`／`large`） | 贊助轉換 |

（金額、分類、幣別都刻意不在表上——就算是「匿名」的金額分佈，也屬於 P3 第 1 點禁止的記帳資料。）

### 3.8 贊助（v2 新增）

- 收入模式是使用者**主動**贊助，**不放廣告**（§4），**贊助不換取任何功能**（不鎖功能、
  不顯示「已贊助」徽章以外的差別待遇）。
- **只能用 Google Play Billing**，做成三個「消耗型」應用程式內商品（可以重複贊助）：
  `donation_small`／`donation_medium`／`donation_large`。實際金額在 Play Console 設定，
  App 顯示 Play 回傳的在地化價格字串，不寫死金額。
- **不得放任何外部付款連結**（PayPal、Buy Me a Coffee、Ko-fi、Open Collective…）。
  Google Play 付款政策要求應用程式內付款使用 Play 結帳系統，並禁止引導使用者到其他付款
  方式；給開發者的贊助不屬於政策排除的「可抵稅慈善捐款」，已有 App 因贊助連結被下架或
  警告的前例。
- 入口在設定頁最下方一個**預設收合**的「支持開發者」區塊，點開才看到三個選項（UI-SPEC §10）。
  首頁、記帳頁、統計頁、底部導覽**不得**出現任何贊助入口或提示。
- 付款完成後顯示感謝訊息；使用者取消付款不顯示任何錯誤；Play 無法連線或商品查不到時，
  顯示「目前無法連線到 Google Play，請稍後再試」，不 crash。

---

## 4. 非目標（v1 明確不做）

寫下來是為了防止 agent 自作主張擴張範圍。

- ❌ 帳號 / 登入 / 雲端同步
- ❌ 廣告（任何形式：橫幅、插頁、獎勵式、原生廣告皆不做）——收入只靠 §3.8 的主動贊助
- ❌ 贊助解鎖功能、付費版、訂閱
- ❌ 畫面錄影類的行為分析（session replay、rage tap 偵測）——記帳畫面全是私密資料
- ❌ 跨錢包幣別換算、總資產彙總
- ❌ 匯率查詢（自動或手動皆不做）
- ❌ 分類層級預算
- ❌ 定期／重複交易
- ❌ 發票掃描、OCR、拍照
- ❌ 複式記帳、轉帳、對帳
- ❌ 多人共享帳本
- ❌ 資料分析預測、AI 建議
- ❌ Widget／捷徑（v1 不做，之後可以是加分項，不是必要項）
- ❌ Wear OS / 平板最佳化排版（v1 只做手機直向，其他 form factor 能跑就好，不用特別優化）

---

## 5. 技術棧

| 用途 | 選擇 | 備註 |
|---|---|---|
| 語言 | Kotlin | strict null-safety；不使用 `!!` 非空斷言，除非有註解說明為何在該處保證安全 |
| UI | Jetpack Compose + Material 3 | |
| 架構 | MVVM（`ViewModel` + `StateFlow`） | |
| DI | Hilt | |
| 本地資料庫 | Room | 封裝在 `data/` |
| 非同步 | Kotlin Coroutines + `Flow` | |
| 日期時間 | `kotlinx-datetime` | `LocalDate` 表示交易日期，避免 `java.util.Date` 的時區陷阱（見 P2/P5 精神延伸） |
| 單元測試 | JUnit 5 | domain 層測試，禁止 mock（純函式不需要） |
| Compose 測試 | Compose UI Testing（`createComposeRule`） | 對應網頁版的 RTL，測試完即拋棄，不算可攜契約 |
| E2E | [Maestro](https://maestro.dev) | YAML 撰寫，一個 flow 對應 `TESTCASES.md` 一個 E2E 案例的 Given/When/Then |
| 靜態分析 | ktlint + detekt | |
| 覆蓋率 | JaCoCo | domain 層門檻 90%，CI 強制 |
| CI/CD | GitHub Actions | `reactivecircus/android-emulator-runner` 起帶 KVM 的 emulator 跑 Compose 測試與 Maestro |
| 本機檔案存取 | Storage Access Framework | 匯出／匯入備份用，取代網頁版的瀏覽器下載/上傳 |
| 本地通知 | `WorkManager` + `NotificationCompat` | 備份提醒用，取代網頁版的頁內 banner |
| App 圖示／啟動畫面 | Android 12+ Splash Screen API | 不需要額外套件 |
| 匿名使用分析（v2） | Firebase Analytics（透過 Firebase BoM） | 只限 §3.7 的事件清單；預設關閉（manifest 關閉自動收集，使用者開啟後才在執行期打開） |
| 當機回報（v2） | Firebase Crashlytics（透過 Firebase BoM） | 同上，預設關閉；不得寫入任何記帳資料到自訂鍵或 log |
| 贊助（v2） | Google Play Billing Library | 只用於 §3.8 的三個消耗型商品 |
| Gradle 外掛（v2） | `com.google.gms.google-services`、`com.google.firebase.crashlytics` | Firebase 必需 |

---

## 6. 開發階段（Phase）

每個 phase 目標 1–2 天，對應一個 PR，必須通過 CI 才能合併。

### Phase 0 — 地基 🔧 *（人工主導，不交給 agent）*

- 建立 GitHub repo
- Gradle（Kotlin DSL）+ Compose + Hilt 骨架，`minSdk` 建議 26（Android 8.0，
  涵蓋率已經很高，且能用完整的 `java.time`／`kotlinx-datetime` API 不用擔心
  desugaring 邊界情況）
- JUnit5 / Compose Testing / Maestro 設定
- ktlint + detekt 規則
- domain 零依賴檢查腳本（純文字掃描，比照網頁版 `check-domain-purity.mjs`）
- GitHub Actions：lint → detekt → 單元測試 + 覆蓋率門檻 → Compose 測試 → 組 debug APK
  → （有 emulator 時）跑 Maestro flow
- 撰寫 `CLAUDE.md`（agent 的常駐指令）與 `TASKS.md`（進度 lock file）
- Branch protection：main 需 PR + CI 綠燈

**驗收：** 一個空白 Compose 畫面成功組出 debug APK 並能在 emulator 上安裝開啟，CI 全綠。

---

### Phase 1 — Domain 核心：金額與時間 💎

**這是整個專案最重要的一個 phase。** 全部是純函式，零 UI。

實作：
- `Money`：整數最小單位（`Long`）、幣別小數位表、加減、格式化、解析
- `Week`：給定日期 + `weekStartDay` → 回傳該週的 `[start, end]` 區間；週分組
- `Month`：給定日期 → 回傳該月的 `[start, end]` 區間

**對應測案：** `TESTCASES.md` T1（Money）、T2（Week）、T5（Month）。

**驗收：** 測案全過，domain 覆蓋率 ≥ 90%，UI 仍為空白畫面。

---

### Phase 2 — Domain：實體與預算計算 💎

實作 `Wallet` / `Transaction` / `Category` 的型別與驗證規則，以及：
- `calculateWeeklyBalance` / `calculateTotalBalance`
- `calculateWeeklyExpenseTotal`
- `summarizeByCategory`
- `summarizeWeeklyTrend`
- 分類刪除規則（`assertCanDeleteCategory`）

**對應測案：** T3（Budget）、T6（Category）。

---

### Phase 3 — 持久層與匯出匯入 💾

- Room schema + migration 機制（schema version 從 1 開始）
- Repository 介面（`domain` 定義介面，`data` 用 Room 實作）
- 匯出／匯入邏輯與 schema 驗證
- Storage Access Framework 串接（匯出寫檔、匯入讀檔）

**對應測案：** T4（Persistence，用 Room in-memory database 測）。

---

### Phase 4 — 基礎 UI：錢包與交易 CRUD 🎨

- 錢包建立／切換／編輯／封存
- 交易列表（依日期分組）、新增、編輯、刪除
- 首次啟動引導：建立第一個錢包
- Compose Navigation 路由骨架

**對應測案：** E2E-1、E2E-2（Maestro）。

---

### Phase 5 — 預算與即時餘額 🎯

- 錢包預算設定（none / weekly / total）
- 主畫面餘額卡片，即時更新（`StateFlow` 訂閱）
- 超支警示樣式
- 週起始日設定

**對應測案：** E2E-3、E2E-4、E2E-5。**這時 app 已具備核心價值。**

---

### Phase 6 — 分類與統計 📊

- 分類管理 CRUD
- 本週／本月分類支出佔比（圓餅圖，Compose Canvas 手畫，不引入圖表套件）
- 近 8 週支出趨勢（長條圖，同樣手畫）

**對應測案：** E2E-8、E2E-9、E2E-10。

---

### Phase 7 — 打磨 ✨

- 匯出／匯入 UI（Storage Access Framework 的實際串接與錯誤處理）
- 備份提醒通知（`WorkManager`）
- 深色模式（沿用 `UI-SPEC.md` 的固定色票，不用 Material You 動態配色——見 §7 D7）
- 空狀態、載入狀態、錯誤處理
- 無障礙基本檢查（TalkBack 能不能正常操作、可點區域 ≥ 48dp）

**驗收：** 所有 E2E 案例全過，手機實機安裝測試通過。

---

### Phase 8 — 上架準備 🚀 *（v2.1 新增）*

讓 App 可以上傳到 Google Play 並開始封閉測試。Google Play 規定：2026-08-31 起新 App 與更新
必須 target API 36；2023-11-13 之後建立的個人開發者帳號，要先完成「至少 12 位測試者、連續
14 天」的封閉測試才能申請正式版。所以這個 phase 排在 Phase 9 之前，讓 14 天的測試期跟
Phase 9 的開發平行進行。

- 錢包封存／取消封存／刪除的 UI（§3.1，v1 漏做的部分）
- `compileSdk`／`targetSdk` 升到 36（連帶升級 AGP），並在 API 36 上驗證 edge-to-edge 強制開啟、
  預測式返回等 Android 16 行為變更
- release 建置：簽章設定（keystore 與密碼**不進 repo**，本機與 CI 從環境變數／GitHub Secret
  讀取）、R8 程式碼與資源壓縮、版本號 1.0.0、產出 AAB
- Auto Backup 的排除規則（§3.6）
- 上架文件草稿（agent 寫草稿，人類在 Play Console 操作）：商店說明文字、隱私權政策（放在
  網頁版 repo 既有的 GitHub Pages）、Data safety／內容分級／目標對象／廣告／財務功能聲明的填寫建議

**對應測案：** E2E-13（錢包封存與刪除）、T4.1.8～T4.1.9（封存規則）、T4.3（備份排除規則）；
既有所有 E2E 在 API 36 emulator 上也要全過。

**驗收：** 所有 E2E 在 API 34 與 API 36 的 emulator 都全過；release AAB 組得出來，開啟 R8 後
主要流程跑過一輪；人類把 AAB 上傳到封閉測試軌道並開始 14 天測試——**Phase 7 欠著的實機驗收
在封閉測試期間一併完成**。

---

### Phase 9 — 匿名使用分析與贊助 💝 *（v2 新增）*

- 設定頁「協助改善 App」開關（預設關閉）＋ Firebase Analytics／Crashlytics 串接，只送 §3.7 表列事件
- 一個集中送事件的 `AnalyticsTracker`（在 `ui`／`data` 層），開關關閉時任何呼叫都是 no-op
- 設定頁「支持開發者」收合區塊 ＋ Google Play Billing 三個消耗型商品
- 更新 Data safety 表單與隱私權政策內容（人類在 Play Console 操作，agent 提供草稿）

**對應測案：** T9（分析開關與事件清單）、E2E-11、E2E-12。

**驗收：** 所有 E2E／T9 全過；人類用 Play Console 的授權測試帳號在實機上完成一次真的贊助
流程；用 Firebase DebugView 確認開關關閉時零事件、開啟後只出現表列事件。

---

## 7. 待確認的設計決策

這些是我在規格中做的判斷，若你不同意請在動工前提出。前六條完全繼承自網頁版
（業務規則沒有因為換平台而改變），第七條是 Android 版特有的。

| # | 決策 | 我的選擇 | 理由 |
|---|---|---|---|
| **D1** | 週餘額要不要扣掉收入？ | **不扣，只算支出** | 「這週還能花多少」的心智模型是支出預算。退款請用「負向處理」：直接編輯或刪除原支出，而非記一筆收入 |
| **D2** | 交易日期是否含時間？ | **只存日期** | 記帳本不需要時分秒，且避免時區地獄。排序用 `createdAt` |
| **D3** | 錢包幣別可否修改？ | **不可** | 已有交易時修改幣別語意不明。要換請建新錢包 |
| **D4** | 週預算未用完是否結轉？ | **不結轉** | 保持簡單。結轉是 v2 候選 |
| **D5** | 分類是全域還是錢包各自？ | **全域共用** | 避免旅遊錢包要重建一套分類 |
| **D6** | 支援哪些幣別？ | **內建常見 20 種 + 使用者自訂代碼** | 純本地無法查表，內建即可。**網頁版直到最後一個 phase 都還沒解決「自訂幣別小數位數」這顆坑**，Android 版動工前建議先想清楚這題，不要重蹈覆轍 |
| **D7** | 深色模式要不要用 Material You 動態配色（跟隨系統桌布抽色）？ | **不用，沿用固定色票** | 保持跟網頁版一致的品牌識別，也讓 `UI-SPEC.md` 的色票可以直接照抄不用另外設計「動態配色版本」。之後想加 Material You 支援可以是獨立的加分項，不影響 v1 架構 |
| **D8** | 匿名分析要預設開啟還是關閉？（v2） | **預設關閉，使用者自己打開** | 記帳是高度私密的資料，「預設不收集」本身就是信任來源；也讓歐盟 GDPR 合規最單純。代價是收到的資料量少很多，接受這個取捨 |
| **D9** | 收入模式？（v2） | **使用者主動贊助，走 Google Play Billing；不放廣告** | 橫幅廣告收益低、會蓋住或擠壓記帳操作，且 AdMob 不允許廣告緊鄰導覽列等常用按鈕；外部贊助連結違反 Play 付款政策 |
| **D10** | Android 系統的 Auto Backup 要保留嗎？（v2.1） | **保留，但排除裝置狀態** | 等於免費、端對端加密、放在使用者自己雲端的換機備份，跟「記帳資料不交給開發者」不衝突；裝置狀態（備份提醒計時、通知權限詢問紀錄、WorkManager 排程）排除，避免新裝置繼承舊裝置的狀態 |

---

## 8. 給 Agent 的工作規則

1. 每次 session 開始：讀 `SPEC.md` + `TESTCASES.md` + `UI-SPEC.md` + `TASKS.md` +
   `git log --oneline -20`
2. 一次只做 `TASKS.md` 中標記為 `NEXT` 的一個 phase
3. 先寫測案並確認失敗，再寫實作
4. 不得修改 `SPEC.md`、`TESTCASES.md` 或 `UI-SPEC.md`；若發現規格矛盾或缺漏，
   在 PR 描述中提出並暫停該項
5. 不得引入 §5 表格以外的相依套件；如需新套件，在 PR 中說明理由並等待批准
6. 每個 PR 只對應一個 phase，開為 **draft PR**
7. CI 失敗時最多自動重試修復 3 次，仍失敗則在 PR 留言說明並停止
8. 完成後更新 `TASKS.md`，將下一個 phase 標為 `NEXT`，並寫交接筆記
