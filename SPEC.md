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
不依賴任何雲端帳號或伺服器，資料完全留在裝置上。

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

### P3. 純本地、零網路

App 執行期間不得發出任何網路請求。無後端、無帳號、無遙測、無崩潰回報 SDK
（Firebase Crashlytics 之類的也不行——這條比網頁版更嚴格，因為 Android
生態很容易「順手」加分析 SDK，必須刻意抵抗）。

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
- App 首次啟動後每 7 天提醒一次備份（用 `WorkManager` 排程本地通知，**不是**
  推播——沒有伺服器，純粹是裝置本地排程）

**匯出格式相容性：** 匯出的 JSON schema 盡量跟網頁版一致（欄位名稱、`schemaVersion`
語意相同），方便同一個使用者未來想把網頁版資料手動搬到 Android 版。這不是 v1
的正式功能（不用做「網頁版直接匯入」的 UI 或文件），純粹是設計 schema 時的一個
念頭，讓兩邊不要無謂地產生不相容的格式。

---

## 4. 非目標（v1 明確不做）

寫下來是為了防止 agent 自作主張擴張範圍。

- ❌ 帳號 / 登入 / 雲端同步
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
