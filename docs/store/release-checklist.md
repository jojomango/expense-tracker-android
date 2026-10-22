# 上架操作清單（人類）

> Phase 8 產出。這些步驟只有你能做（涉及你的帳號、身分、金鑰），agent 已經把程式碼與
> 文件草稿準備好。照順序做即可。

## 0. 現在就可以開始（最花時間的兩件事）

- [ ] 申請 Google Play 開發者帳號（個人），完成身分驗證——可能需要數天
- [ ] 找測試者：規定至少 **12 位、連續 14 天**。建議找 **15～20 位**：只要人數中途跌破 12，
      14 天就重新計算。需要的是他們的 Google 帳號 email

## 1. 建立上傳金鑰（只做一次，**務必備份**）

在你自己的電腦上執行（密碼請自己設定、不要貼給任何人）：

```bash
mkdir -p ~/keys
keytool -genkeypair -v -keystore ~/keys/expense-tracker-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

把下面四行加到 **`~/.gradle/gradle.properties`**（你家目錄底下的那個，**不是** repo 裡的）：

```properties
EXPENSE_UPLOAD_STORE_FILE=/Users/你的帳號/keys/expense-tracker-upload.jks
EXPENSE_UPLOAD_STORE_PASSWORD=你設定的 keystore 密碼
EXPENSE_UPLOAD_KEY_ALIAS=upload
EXPENSE_UPLOAD_KEY_PASSWORD=你設定的 key 密碼
```

- `.jks` 檔和密碼**不要放進 repo**（`.gitignore` 已經擋了 `*.jks`）。
- 把 `.jks` 和密碼另外備份到安全的地方（例如密碼管理器）。啟用 Play App Signing 之後，
  遺失上傳金鑰可以向 Google 申請重設，但會很麻煩。

## 2. 組出要上傳的 AAB

```bash
./gradlew :app:bundleRelease
```

產出在 `app/build/outputs/bundle/release/app-release.aab`。

確認它是用**你的上傳金鑰**簽的（沒設定 `~/.gradle/gradle.properties` 的話會退回 debug 金鑰，
Play Console 會拒收）：

```bash
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

輸出的「擁有者」應該是你在步驟 1 填的資訊，**不是** `CN=Android Debug`。

## 3. Play Console：建立 App

- [ ] 建立應用程式：名稱「記帳本」、預設語言「中文（台灣）」、App（不是遊戲）、免費
- [ ] 啟用 **Play App Signing**（建立時預設會啟用）
- [ ] 這個 App 的**商店聯絡信箱**（「商店設定 → 商店資訊聯絡詳細資料」，顯示在商店頁面上）填
      jojomango99@gmail.com。建議在 99 設自動轉寄到你常用的信箱，使用者來信才不會漏看
- [ ] 開發人員**帳戶**的聯絡信箱（帳戶詳細資料，Google 聯絡你、帳戶驗證用）可以維持你原本註冊用的個人信箱，
      不用改；帳戶驗證進行中時先不要動帳戶資料
- （欄位名稱以 Console 實際畫面為準）

### 上傳時的「未上傳偵錯符號檔」警告：可以忽略

上傳 AAB 時 Play 會警告「這個 App Bundle 含有原生程式碼，而您尚未上傳偵錯符號檔」。AAB 裡的原生函式庫只有兩個，
都不是我們寫的，而且**從來源 AAR 就已經 strip 掉符號**：

- `libandroidx.graphics.path.so` ← `androidx.graphics:graphics-path:1.0.1` ← Compose `ui-graphics`
- `libdatastore_shared_counter.so` ← `androidx.datastore:datastore-core:1.1.7` ← `firebase-sessions` ← Crashlytics

2026-10-22 實測過：release 加上 `ndk { debugSymbolLevel = "SYMBOL_TABLE" }` 後，AGP 的
`extractReleaseNativeSymbolTables` 產出 0 個檔案、AAB 裡也沒有 `debugsymbols`，所以沒有加這個設定。
`FULL` 需要原始偵錯資訊，更不可能產生。這則警告只是「警告」，不擋發布；Kotlin 的當機堆疊由 Crashlytics
＋R8 mapping 處理，不受影響。之後若專案加入自己的 C/C++ 程式碼，再重新評估。

## 4. 先上「內部測試」確認上傳流程

內部測試不需要審核、幾分鐘內就能裝，適合先確認 AAB 沒問題：

- [ ] 測試 → 內部測試 → 建立新版本 → 上傳 `app-release.aab`
- [ ] 把自己加進測試人員，用自己的手機從 Play 商店裝起來跑一輪：
      **建立錢包 → 記帳 → 看統計 → 匯出備份 → 清除資料 → 匯入還原**，再開 TalkBack 操作一次記帳
      （**這就是 Phase 7 一直還欠著的實機驗收**，跑完請告訴我結果）

## 5. 應用程式內容（封閉測試送審前必填）

照 `docs/store/play-console-answers.md` 的建議填：

- [ ] 隱私權政策網址（見步驟 6）
- [ ] 廣告：否
- [ ] 應用程式存取權：不需要特殊存取權
- [ ] 內容分級問卷
- [ ] 目標對象：建議 18 歲以上
- [ ] 資料安全性：這一版「不收集任何資料」
- [ ] 財務功能聲明：不提供財務功能

## 6. 隱私權政策網址

- [ ] GitHub 這個 repo 的 **Settings → Pages**：Source 選 `Deploy from a branch`，分支 `main`、資料夾 `/docs`，存檔
- [ ] 一兩分鐘後確認 `https://jojomango.github.io/expense-tracker-android/privacy-policy.html` 打得開
      （`docs/_config.yml` 排除了 `docs/store/`，這些作業筆記不會出現在網站上）
- [ ] Play Console 的「隱私權政策」與「資料安全性」填這個網址

## 7. 商店資訊

- [ ] 照 `docs/store/listing.md` 填簡短說明與完整說明
- [ ] 上傳圖示、主題圖片、截圖（截圖要的話我可以用 emulator 產生）

## 8. 封閉測試（14 天）

- [ ] 測試 → 封閉測試 → 建立測試群組，加入測試者的 email
- [ ] 上傳同一個（或更新的）AAB，送審
- [ ] 審核通過後把加入連結傳給測試者，**確認每個人都有接受邀請並安裝**（只發邀請不算）
- [ ] 14 天期間 Phase 9（分析與贊助）會平行開發，做完直接更新到同一個封閉測試軌道

## 9. 正式版

- [ ] 14 天到期後，在資訊主頁申請正式版存取權
- [ ] 通過後建立正式版並發布
