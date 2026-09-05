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

### 2. Mac 上的 Android emulator（arm64）

Apple Silicon Mac 建 AVD（Android Virtual Device）時，系統映像一定要選
**ARM 64 v8a**，不要選 `x86_64`——選錯的話 emulator 沒有硬體加速會跑不動
或開機逾時。

**這條路已經在本機實際跑通過**（不只是理論上可行），踩過的坑記在這裡：

- `emulator` 這個 SDK package 本身也分 x86_64／arm64 兩種 build，跟系統映像
  是分開的兩件事。如果 `sdkmanager` 是用 x86_64 版本的 JVM 跑的（例如
  Homebrew 裝的 JDK 剛好是 x86_64 build，即使機器本身是 Apple Silicon），
  `sdkmanager` 會誤判 host 架構，連帶把 `emulator` package 也裝成 x86_64
  版本——這樣即使系統映像選對 arm64，emulator 開機還是會噴
  `FATAL | Avd's CPU Architecture 'arm64' is not supported by the QEMU2
  emulator on x86_64 host`。用 `file "$(which java)"` 確認 JAVA_HOME 指向
  的 JDK 真的是 `arm64` build，不是 `x86_64`。
- 舊版 `cmdline-tools`（例如 `cmdline-tools;3.0`）太舊，太舊到根本不認得
  arm64 Mac 這個 host，`sdkmanager --list` 連 `emulator` package 都列不
  出來。到 [Android 官方下載頁](https://developer.android.com/studio#command-line-tools-only)
  抓最新版 `commandlinetools-mac-*.zip`（或直接開 Android Studio 讓它
  自動更新），再用新版 `sdkmanager "emulator"` 重裝一次。
- 用 Android Studio 走 GUI（Device Manager → Create Device → 系統映像選
  **ARM 64 v8a**）通常會自動避開上面兩個坑，是最省事的路；上面兩點是給
  用命令列工具、或懷疑本機環境有問題時排查用的。
- 開機起來之後：
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
  或在 Android Studio 按 Run 直接裝進這台 emulator。
- 這台本機 emulator 也可以直接拿來跑 `.maestro/` 的 Maestro flow
  （`maestro test .maestro/`，需要 Maestro CLI 用 Java 17+），比等 CI
  快很多，還能用 `adb shell input tap`／`maestro hierarchy` 即時對照
  畫面除錯。

### 3. 讓 CI 自動跑一遍 Maestro flow

```bash
gh workflow run e2e.yml --ref main
```

這會讓 GitHub Actions（`ubuntu-latest` + KVM 加速的 emulator）照著
`.maestro/*.yaml` 自動操作一遍、回報過或沒過。`e2e.yml` 是獨立的 workflow
檔案，只在 push 到 `main` 或手動 `workflow_dispatch` 時才會跑，不會出現在
一般 PR 的 checks 清單裡。這個方式看不到畫面本身，適合當「有沒有壞掉」的
煙霧測試，不能取代前兩種方式的實際手動驗收。
