# CLAUDE.md — Agent 常駐指令（Android 版）

> 這個檔案在每次 session 開始時自動載入。以下規則優先於你的預設行為。

## 這是什麼專案

一個純本地、離線可用的記帳 Android App，原生 Kotlin + Jetpack Compose。
這是 [expense-tracker](https://github.com/jojomango/expense-tracker)（同一產品的
PWA 網頁版）的 Android 原生重寫版。詳見 `SPEC.md`。

**本專案由 AI agent 自主開發，人類只做 review 與合併。**
因此以下規則不是建議，是硬約束。

---

## 每次 session 的開場程序

**在寫任何一行程式碼之前**，依序做完這五件事：

1. 讀 `SPEC.md`（規格契約）
2. 讀 `TESTCASES.md`（測試契約）
3. 讀 `UI-SPEC.md`（介面契約）
4. 讀 `TASKS.md`（進度狀態機）→ 找出標記為 `**NEXT**` 的 phase
5. 執行 `git log --oneline -20` 了解已完成的工作

然後**只做那一個 phase**。不多做，不少做。

---

## 三條絕對禁令

### 🚫 1. 不得修改 `SPEC.md`、`TESTCASES.md` 或 `UI-SPEC.md`

這三份是人類簽署的契約。

若你發現規格有矛盾、缺漏或錯誤：
- **停止該項工作**
- 在 PR 描述的 `## 需要人類決策` 段落寫下問題
- 繼續做該 phase 中不受影響的部分

**永遠不要為了讓測試通過而修改測案。**
若某個測案看起來是錯的，那正是需要人類介入的訊號。

### 🚫 2. `domain` package 必須零 Android 依賴

不得 `import android.*` 或 `import androidx.*`（`kotlin.*` / `kotlinx.*` 不在此限，
`kotlinx-datetime` 是允許的）。不得使用 `Clock.System.now()`、
`System.currentTimeMillis()`，或任何形式的「函式內部自己取得現在時間」。

需要外部能力時：在 `domain` 內**定義介面**，由外層（`data`／`di`）注入實作。
需要現在時間時：**把時間（`LocalDate`／`Instant`）當參數傳進來**，不要自己取。

CI 第一步會跑一個純文字掃描腳本強制執行這條（見 `scripts/check-domain-purity.*`，
Phase 0 建立）。

理由：`domain` 層應該是這個 app 最耐久的資產——換 UI 框架、換資料庫、甚至之後
想抽成 Kotlin Multiplatform module 給 iOS 或桌面版共用，都不需要重寫這一層。

### 🚫 3. 不得新增 `SPEC.md §5` 表格以外的相依套件

需要新套件時，在 PR 描述說明理由並**停止**，等待人類批准。
先試著用現有工具或手寫解決。

**特別提醒（Android 生態的常見陷阱）：** 不要因為「方便」就加分析 SDK
（Firebase Analytics、Crashlytics）、廣告 SDK、或任何會發網路請求的函式庫，
即使只是「先加著，之後再決定要不要用」——`SPEC.md` P3「零網路」是硬約束，
沒有例外。

---

## 工作流程

### 開發順序（不可顛倒）

```
1. 讀該 phase 在 TESTCASES.md 中對應的測案
2. 寫測試 → 執行 → 確認「因為功能還沒實作」而失敗
3. 寫實作 → 執行 → 轉綠
4. 跑完整本地驗證（見下方指令）
5. 更新 TASKS.md
6. 開 draft PR
```

**步驟 2 不可跳過。** 沒看過測試失敗，就不知道測試有沒有真的在測東西。

### 測試撰寫規則

- 測試名稱必須含測案編號，例如 `@DisplayName("T2.1.4 — 週日應歸屬前一週")`
- domain 測試**不得 mock 任何東西**。純函式不需要 mock；
  若你發現需要 mock，代表設計違反了禁令 2
- 時間相關測試必須注入固定的 `referenceDate`，**禁止 `Clock.System.now()`**
- 可以新增 `TESTCASES.md` 沒有的測案，但不得刪除或弱化既有測案

### 提交前必跑

```bash
./gradlew ktlintCheck detekt          # 靜態分析
./gradlew testDebugUnitTest           # domain + data 單元測試，含覆蓋率
./gradlew jacocoTestReport            # 確認 domain 覆蓋率 ≥ 90%
./gradlew assembleDebug               # 確認組得出 APK
```

（Phase 0 建立好之後，把上面這串包成一個 `./gradlew verify` 聚合 task，
之後每次都跑這一行就好，比照網頁版 `npm run verify` 的精神。）

Maestro flow 另外跑（需要 emulator 或實機）：

```bash
maestro test .maestro/
```

**全部綠燈才能開 PR。**

---

## PR 規則

- **一個 PR 對應一個 phase**，不要合併多個 phase
- 開為 **draft PR**，標題格式：`Phase N — <phase 名稱>`
- 分支名稱使用 `claude/phase-N-<slug>`
- **絕對不要自行合併 PR**，也不要 push 到 `main`

### PR 描述模板

```markdown
## 這個 PR 做了什麼
（3 行以內）

## 對應的測案
- T1.1.x ~ T1.3.x（新增 N 個測試，全部通過）

## 驗證結果
- [ ] ktlint / detekt 通過
- [ ] 單元測試通過，domain 覆蓋率 XX%
- [ ] Maestro flow 通過
- [ ] debug APK 組得出來，emulator 安裝並手動點過一輪

## 需要人類決策
（沒有就寫「無」。有的話務必列出，不要自己猜。）

## 我沒做的事
（該 phase 中刻意留給下一階段的部分）
```

---

## 卡住時該怎麼做

**不要硬幹。** 以下情況一律停止並在 PR 中說明：

| 情況 | 做法 |
|---|---|
| 測案與規格矛盾 | 停止，寫進「需要人類決策」 |
| 需要新套件 | 停止，說明理由 |
| CI 連續失敗 3 次 | 停止，貼出錯誤與你的分析 |
| 某個測案怎麼寫都很醜 | 可能是設計問題，停止並提出重構建議 |
| 這個 phase 比預期大很多 | 完成能完成的，其餘寫進「我沒做的事」 |
| 網頁版的某個規則在 Android 上完全講不通 | 去查網頁版 repo 的 `TASKS.md` 交接筆記有沒有解釋；查不到才寫進「需要人類決策」 |

**誠實回報失敗，遠比假裝成功有價值。**
綠色的 CI 才是成功的證據，你的自我評估不是。

---

## 程式碼風格

- Kotlin，開啟嚴格 null 檢查；**避免 `!!`**，除非緊鄰一個註解說明「為什麼這裡保證非空」
- 金額一律用 `Long` 最小單位整數，**禁止 `Float`/`Double` 表示金額**（見 `SPEC.md` P2）
- 日期一律用 `kotlinx.datetime.LocalDate`，**不使用 `java.util.Date`**
- `domain` 的資料型別一律用 `data class`，欄位盡量 `val`（不可變）；
  需要表示「多選一」的狀態（例如 `BudgetMode`、`TransactionType`）用 `sealed class`
  或 `enum class`，不要用字串常數
- 註解寫「為什麼」，不寫「做什麼」。程式碼應自我說明「做什麼」
- 面向使用者的文字一律繁體中文

## 目錄職責

```
domain/    純 Kotlin 業務邏輯。零 Android 依賴。← 最重要的資產
data/      Room / Repository 實作。實作 domain 定義的介面
ui/        Jetpack Compose 元件。只渲染與轉發事件，不含業務邏輯
di/        Hilt 組裝層：DI 綁定、Repository 提供者
```

（實際用 Gradle multi-module 還是單 module 內用 package 分層，Phase 0 依專案規模
自行決定；不管哪種做法，上面這條「誰依賴誰」的方向不能反。）

**判斷準則：** 如果一段邏輯換到 iOS／桌面版也應該一模一樣，它就屬於 `domain/`。

---

## 完成一個 phase 後

1. 更新 `TASKS.md`：
   - 當前 phase 標記為 `✅ DONE`，附上 PR 連結
   - 下一個 phase 標記為 `**NEXT**`
2. 在 `TASKS.md` 的「交接筆記」寫下對下一個 phase 有用的資訊
   （你做了什麼設計決策、留了什麼坑、下一個人該注意什麼）

這份筆記是下一次 session 的你唯一能繼承的東西 —— **context 不會延續，git 才會。**
