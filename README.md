# 記帳本 (Android)

[expense-tracker](https://github.com/jojomango/expense-tracker)（PWA 網頁版記帳本）的
Android 原生重寫版本。Kotlin + Jetpack Compose，純本地、離線可用、零網路。

本專案由 AI agent 自主開發，人類負責 review 與驗收——規格、測案、UI 規格、
進度狀態機分別在 [SPEC.md](SPEC.md)、[TESTCASES.md](TESTCASES.md)、
[UI-SPEC.md](UI-SPEC.md)、[TASKS.md](TASKS.md)，開發規則在 [CLAUDE.md](CLAUDE.md)。

## 建置

```bash
./gradlew assembleDebug
```

APK 產出在 `app/build/outputs/apk/debug/app-debug.apk`。

```bash
./gradlew verify
```

跑完整本地驗證（domain 純淨度檢查、ktlint/detekt 靜態分析、單元測試 + 覆蓋率、組 APK）。

## 怎麼驗收

這不是網頁版——沒有一個網址可以點開看。要看到、操作到目前這個版本的畫面，
有三條路：

### 1. 裝到實體 Android 手機（最快、最直接）

1. 手機上打開「開發者選項」→「USB 偵錯」
   （設定 → 關於手機 → 連續點「版本號碼」7 次會解鎖開發者選項）
2. USB 接上電腦，跑：
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. 手機上直接點開 App 圖示，跟操作一般 App 完全一樣。

### 2. Mac 上的 Android emulator

Apple Silicon Mac 建 AVD（Android Virtual Device）時，系統映像一定要選
**ARM 64 v8a**，不要選 `x86_64`——選錯的話 emulator 沒有硬體加速會跑不動
或開機逾時（這也是本機開發過程中一直遇到的問題）。

1. Android Studio → Device Manager → Create Device → 選一支手機
2. 系統映像選 **ARM 64 v8a**（可能需要先在 SDK Manager 下載）
3. 開機完成後，在 Android Studio 按 Run，或跑 `./gradlew installDebug` 裝進這台 emulator

### 3. 讓 CI 自動跑一遍 Maestro flow

```bash
gh workflow run e2e.yml --ref main
```

這會讓 GitHub Actions（ubuntu-latest + KVM 加速的 emulator）照著
`.maestro/*.yaml` 自動操作一遍、回報過或沒過。這個方式看不到畫面本身，
適合當「有沒有壞掉」的煙霧測試，不能取代前兩種方式的實際手動驗收。
