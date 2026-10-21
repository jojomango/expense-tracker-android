# Play Console「應用程式內容」填寫建議（對應版本 1.1.0）

> 草稿（Phase 9 更新）。這是根據**這一版**的實際行為寫的建議，不是 Play 的官方解釋——
> 填寫時以 Console 上的題目與說明為準。標「⚠️ 需確認」的是我查不到明確官方說法的地方。
> 1.1.0 加入了預設關閉的匿名使用分析（Firebase）與 Google Play 贊助：**資料安全性、隱私權政策、
> 內容分級的「數位商品」題目都要依下面重填**，再上傳 1.1.0。

## 隱私權政策

商店聯絡信箱：jojomango99@gmail.com（這個 App 的「商店設定 → 商店資訊聯絡詳細資料」，顯示在商店頁面上）。
開發人員帳戶本身的聯絡信箱可以維持原本註冊用的個人信箱（不公開），兩者是不同欄位。

網址：`https://jojomango.github.io/expense-tracker-android/privacy-policy.html`（這個 repo 的 GitHub Pages，來源是 `docs/privacy-policy.html`）。

## 廣告

**否，App 不含廣告。**（SPEC.md §4：任何形式的廣告都不做。）

## 廣告 ID 聲明

**App 不使用廣告 ID。** Firebase Analytics 會自動帶進 `AD_ID` 權限，`AndroidManifest.xml` 用
`tools:node="remove"` 拿掉了（`AnalyticsManifestTest` 會檢查）；合併後的 manifest 也確認過沒有。

## 應用程式存取權

**所有功能都不需要特殊存取權限即可使用**（沒有登入、沒有帳號、沒有付費牆）。

## 內容分級（IARC 問卷）

- 類別：實用工具／生產力類（不是遊戲）
- 暴力、性、髒話、管制物品、賭博：全部「否」
- 使用者之間能否互動或分享內容：否
- 是否分享使用者的位置：否
- 是否可以在 App 內購買數位商品：**是**（1.1.0 起有贊助的消耗型商品）

## 目標對象與內容

- 建議目標年齡層：**18 歲以上**。選到 13 歲以下會適用「家庭」政策的額外規定；這是一般用途的記帳工具，
  沒有特別針對兒童或青少年，選 18 歲以上最單純。
- App 是否可能吸引兒童：否

## 資料安全性

1.1.0 建議的回答。重點：**「協助改善 App」預設關閉、使用者自己打開才收集**，所以下面每一項的
「收集是否為必要」都選**選填（使用者可以選擇是否提供）**。

| 題目 | 建議回答 | 理由 |
|---|---|---|
| App 是否收集或分享任何必要的使用者資料類型？ | **是** | 使用者打開「協助改善 App」後，Firebase 會收集下表的資料 |
| 所有資料在傳輸過程中是否加密？ | **是** | Firebase 與 Google Play 都走 HTTPS |
| 是否提供讓使用者要求刪除資料的方式？ | **是** | 關閉開關會清除手機上的分析識別碼；也可以寫信到 jojomango99@gmail.com 要求刪除 |

**收集的資料類型**（全部：收集、不分享、選填、用途「分析」；當機相關的再勾「App 功能」）：

| Console 的類別 | 資料類型 | 來源 |
|---|---|---|
| App 活動 | App 互動 | SPEC.md §3.7 的事件，加上 Firebase 自動收集的 first_open、session_start、user_engagement 等 |
| App 資訊與效能 | 當機記錄 | Crashlytics |
| App 資訊與效能 | 診斷 | Crashlytics 附帶的裝置型號、系統版本、記憶體狀態 |
| 裝置或其他 ID | 裝置或其他 ID | Firebase 的 app instance ID／installation ID（不是廣告 ID） |

- **「分享」選否**：Google 的說明是，交給代你處理資料的服務供應商（Firebase）不算分享。⚠️ 需確認 Console 的最新說明。
- **大概位置**：Firebase 會用 IP 推估國家／地區。⚠️ 需確認——要不要勾「大概位置」請以 Firebase 官方的
  「Google Play 資料安全性」說明頁（Firebase 文件裡每個產品各有一節）為準，我沒有查到確定的答案。
- **購買記錄：** 付款由 Google Play 處理，開發者只拿到 Play Console 的訂單紀錄。⚠️ 需確認 Console 是否要求勾
  「財務資訊 → 購買記錄」；保守的做法是勾，用途「App 功能」。
- **不收集**：個人資訊、財務資訊（帳戶、信用卡）、位置、聯絡人、相片、檔案、記帳內容。記帳資料一律不離開裝置。
- **Android 系統的 Auto Backup 不算「收集」**（人類 2026-10-18 決定）：資料是 Android 系統服務備份到
  **使用者自己的** Google 帳號，開發者拿不到，所以不需要申報。

### Firebase 專案的設定（人類操作，影響上面的回答）

- **不要**把 Firebase 連結到 Google Play（Firebase Console → 專案設定 → 整合）。Firebase 的自動收集事件裡有
  `in_app_purchase`（含商品與金額），不在 SPEC.md §3.7 的清單上。⚠️ 我不確定不連結 Play 是否就不會出現——
  驗收時用 DebugView 完成一筆測試贊助，看有沒有 `in_app_purchase`（見 PR 的「需要人類決策」）。
- **不要**開 Google Signals、不要連結 Google Ads。
- GA4 資源的「資料保留」設成 **2 個月**（最短），隱私權政策說「依保存期限自動刪除」。

## 財務功能聲明

**本 App 不提供任何財務功能**（不處理付款、轉帳、借貸、投資，只是記錄使用者自己輸入的收支）。
贊助走 Google Play Billing，是一般的應用程式內購，不算「財務功能」。⚠️ 需依 Console 上的選項再確認一次。

## 新聞、政府、健康、COVID-19 等其他聲明

全部「否」或「不適用」。

## 目標 API 與其他技術項目（agent 已處理）

- `targetSdk 36`（Google Play 2026-08-31 起的要求）
- 16 KB 分頁大小：唯一的原生函式庫 `libandroidx.graphics.path.so` 已確認以 16 KB 對齊
- R8 已開啟並保留行號，Android vitals 的當機堆疊對得回原始碼
- 上傳格式：AAB（`app/build/outputs/bundle/release/app-release.aab`）
