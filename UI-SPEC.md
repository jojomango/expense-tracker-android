# UI-SPEC.md — 介面契約（Android 版）

> 這份檔案與 `SPEC.md`、`TESTCASES.md` 同級：**agent 不得修改**。
> 發現規格矛盾時，停止該項工作並寫進 PR 的「需要人類決策」。
>
> `CLAUDE.md` 的開場程序已經把讀這份檔案排進去了。

`SPEC.md` 定義這個 app 做什麼，這份定義它長什麼樣子。所有數值都是確切值，不是建議值。
牴觸時以 `SPEC.md` 為準（例如 §3 的資料規則），但外觀一律以本檔為準。

**這份文件是網頁版 `UI-SPEC.md`（Phase 8～10 UI 改版契約）的 Android／Compose 版本**——
色票、字級、間距、圓角、動畫時間**數值完全相同**，只是把「怎麼實作」從 Tailwind CSS
換成 Jetpack Compose 的對應寫法。目的是讓兩個平台的視覺語言一致，使用者從網頁版切
到 Android 版時不會有「這是另一個 app」的違和感。

---

## §1 設計原則

1. **手機優先。** 設計基準 390×844 dp（等同網頁版的 iPhone 14 基準，Android 上用 dp
   而非 px，數值直接沿用）。平板只需可用，不需最佳化（見 `SPEC.md` §4 非目標）。
2. **底部導覽。** 主要導覽在拇指區，不在頂端。所有可點目標 ≥ 48×48 dp
   （Android 官方無障礙建議值是 48dp，比網頁版的 44px 略大——**用 48dp，不要照抄
   44**，這是本文件唯一一處刻意跟網頁版數值不同的地方，因為換了平台的官方指引）。
3. **一個畫面一個主角。** 首頁的主角是本週可用餘額，記帳頁的主角是金額，統計頁的
   主角是期間總額。
4. **不用純文字連結當按鈕。** 次要動作用 `FilledTonalButton`／`TextButton`／
   `IconButton`／列表列，不要在 `Text` 上手動加底線模擬連結感。
5. **數字用等寬數字。** 所有金額用 `Typography` 的 `FontFeatureSettings("tnum")`
   或直接選用內建等寬數字的字重，避免跳動。
6. **深淺色同等對待。** 每個顏色都必須有 light / dark 兩個值，不接受只在其中一版可讀。

---

## §2 設計 token

在 Compose 裡，這些 token 應該定義成 `Theme.kt` 裡的 `ColorScheme` / `Typography` /
`Shapes`，透過 `MaterialTheme` 往下傳，**不要在個別 Composable 裡寫死顏色或字級數值**。

### §2.1 顏色

| token（對應 Compose `ColorScheme` 欄位建議） | light | dark | 用途 |
|---|---|---|---|
| `background` | `#F2F1EF` | `#000000` | 分組背景（頁面底色） |
| `surface` | `#FFFFFF` | `#1C1C1E` | 卡片、列表列 |
| `onBackground` / `onSurface` | `#111114` | `#FFFFFF` | 主要文字、金額 |
| `onSurfaceVariant`（fg2） | `#6B6B70` | `#98989F` | 次要文字、標籤 |
| （fg3，Compose 沒有直接對應欄位，自訂一個 `LocalContentColor` 變體） | `#A3A3A8` | `#6C6C72` | 第三層文字、未選取的分頁圖示 |
| `outlineVariant`（sep） | `#E6E3DF` | `#2C2C2E` | 分隔線、卡片內分隔 |
| （track，自訂 token） | `#ECE9E6` | `#2C2C2E` | 進度條底、分段控制底 |
| （barbg，自訂 token，需帶透明度） | `rgba(242,241,239,0.86)` | `rgba(10,10,12,0.80)` | 底部導覽列背景（`Modifier.blur` 或 `Surface` 的 `tonalElevation` 模擬毛玻璃） |
| （sheet，自訂 token） | `rgba(255,255,255,0.96)` | `rgba(44,44,46,0.96)` | `ModalBottomSheet` 背景 |
| （keypad，自訂 token） | `#E2DFDA` | `#151517` | 數字鍵台背景 |
| （key，自訂 token） | `#FFFFFF` | `#3A3A3C` | 數字鍵 |
| `primary`（accent） | `#C1502E` | `#D9673F` | 主鍵、選取態、進度條、當週柱 |
| `error`（danger） | `#D9463B` | `#E8564A` | 刪除動作、超支 |
| （income，自訂 token） | `#2F8F63` | `#3FA878` | 收入金額 |

dark 的 accent 比 light 亮一階，因為 `#C1502E` 在純黑上對比不足。

**實作建議：** Compose Material 3 的 `ColorScheme` 有固定欄位名（`primary`／
`background`／`surface`／`error`⋯），上面括號標「自訂 token」的幾個沒有對應欄位，
建議在 `Theme.kt` 額外定義一個 `data class AppExtraColors(val track: Color, val barBg: Color, ...)`，
透過 `CompositionLocalProvider` 跟 `MaterialTheme` 一起往下傳，而不是硬塞進
`ColorScheme` 不相關的欄位（例如不要把 `track` 塞進 `secondary`，語意會混亂）。

### §2.2 分類固定色

分類色綁在分類本身，不隨排序變動（絕對不要用「依金額排序後的 index」決定顏色，
同一分類在不同期間顏色會跳來跳去，失去記憶點）。`DEFAULT_CATEGORIES` 的種子色：

| 分類 | icon | color |
|---|---|---|
| 飲食 | 🍜 | `#C1502E` |
| 交通 | 🚗 | `#3F8F6A` |
| 居住 | 🏠 | `#A8792F` |
| 購物 | 🛒 | `#2F6F9F` |
| 娛樂 | 🎬 | `#8A5FBF` |
| 醫療 | 💊 | `#C04A6E` |
| 其他（支出） | 📦 | `#7A7A80` |
| 薪資 | 💰 | `#2F8F63` |
| 獎金 | 🎁 | `#C98B2E` |
| 投資 | 📈 | `#4A6FA8` |
| 其他（收入） | 📦 | `#7A7A80` |

未分類（`categoryId == null`）一律 `#7A7A80` + 📦。

**淡色底（tint）**：分類色 + 透明度 —— light `12%`、dark `18%`。用於分類圖示的方形
色塊。Compose 寫法：`categoryColor.copy(alpha = if (isDark) 0.18f else 0.12f)`。

### §2.3 字級

系統字體（不嵌入自訂字型檔）：Compose 預設的 `FontFamily.Default` 在 Android 上就是
系統字體（Roboto 或 OEM 客製字體），不需要額外處理，這點比網頁版簡單（網頁版要手動
指定字體堆疊避免載入 webfont）。

| 名稱 | size / weight / letter-spacing（對應 `TextStyle`） | 用途 |
|---|---|---|
| `balance` | 46sp / W600 / -0.03em | 首頁餘額 |
| `amountInput` | 54sp / W600 / -0.035em | 記帳頁金額 |
| `titleLarge` | 30sp / W700 / -0.02em | 統計頁大標題 |
| `donutTotal` | 27sp / W600 / -0.02em | 圓環中心總額 |
| `navTitle` | 17sp / W600 | 首頁錢包名稱 |
| `rowAmount` | 17sp / W500 | 列表金額 |
| `rowTitle` | 16sp / W500 | 列表分類名 |
| `cardTitle` | 15sp / W600 | 卡片標題 |
| `bodyMedium` | 14sp / W400 | 圖例、sheet 內文 |
| `caption` | 13sp / W400 | 次要說明、列表副標 |
| `label` | 12sp / W500 | 卡片小標籤 |
| `tabLabel` | 10sp / W400 | 分頁列文字 |

所有金額 `Text`：套用等寬數字（`style.copy(fontFeatureSettings = "tnum")`）。

這些應該定義成 `Typography` 裡具名的 `TextStyle`（例如 `MaterialTheme.typography.balance`
需要透過擴充 `Typography` 或自訂一個 `AppTypography` object 來達成，Material 3 的
內建 `Typography` 欄位是固定命名的 `displayLarge`／`bodyMedium` 這類語意名稱，
兩種做法都可以，選一種團隊統一用）。

### §2.4 尺寸與間距

- 頁面左右內距 **20dp**；卡片內距 **22dp**；列表列內距 **11dp / 14dp**（垂直 / 水平）
- 卡片圓角 **20dp**；列表群組圓角 **16dp**（第一列上圓角、最後一列下圓角，Compose
  用 `RoundedCornerShape` 分別指定 `topStart`/`topEnd`/`bottomStart`/`bottomEnd`）；
  分類色塊 **12dp**；數字鍵 **12dp**；pill **999dp**（等同 `CircleShape` 或超大圓角值）；
  toast/Snackbar **14dp**
- 卡片陰影：Compose 用 `tonalElevation` + `shadowElevation`（Material 3 的陰影系統
  跟 CSS `box-shadow` 概念不同，不用硬換算成同樣的 `rgba` 數值，用 `elevation = 2.dp`
  左右取一個視覺上接近的值即可）；FAB 用預設的 `FloatingActionButton` elevation
- 狀態列高度：交給系統（`WindowInsets.statusBars`），不要手動寫死高度；
  底部導覽列高 **84dp**（含 `WindowInsets.navigationBars` 的安全區）
- 列表列最小高 **44dp**（沿用網頁版數值，這條不受 §1.2 的 48dp 規則影響，因為
  列表列本身有文字排版撐開高度，通常自然 ≥ 48dp；但如果整列只是純點擊區、
  沒有足夠內容撐高，仍要用 `Modifier.heightIn(min = 48.dp)` 補到 48dp）；
  分類圖示 **38×38dp**（列表）/ **50×50dp**（記帳頁網格）；FAB **58×58dp**
- 進度條高 **8dp**（首頁）/ **4dp**（圖例）；`LinearProgressIndicator` 的
  `strokeCap = StrokeCap.Round` 讓圓角視覺一致

### §2.5 動畫

- 左滑刪除位移：`220ms`，`CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)`，用 Compose
  的 `Animatable` 或 `SwipeToDismissBox`（Material 3 內建元件，優先用這個，不要
  手刻手勢邏輯——這點跟網頁版不同，網頁版沒有現成的左滑元件所以手刻 Pointer
  Events，Android 有內建的就直接用）
- Bottom sheet 進場：`ModalBottomSheet` 內建的進場動畫，不需要手刻，數值上
  跟網頁版的 `260ms` 接近即可，不用像素級對齊
- Snackbar（對應網頁版的 toast）：系統內建的 `SnackbarHost` 淡入淡出與停留時間，
  停留時長透過 `SnackbarDuration` 設定，找最接近 **2600ms** 的選項或自訂
  `withDismissAfter`
- 尊重系統的「移除動畫」無障礙設定（Android 的 `Settings.Global.ANIMATOR_DURATION_SCALE`），
  Compose 的動畫 API 預設就會尊重這個系統設定，不需要額外處理

---

## §3 骨架

### §3.1 底部導覽（`NavigationBar`）

用 Compose Material 3 的 `NavigationBar` + `NavigationBarItem`，不要手刻。

三個項目，由左至右：

1. **首頁** — 路由 `/`，圖示用 `Icons.Outlined.Home` 風格的簡單圖示（不強求跟網頁版
   手畫的方框圖示像素對齊，Android 用 Material Icons 就好）
2. **記帳** — 中央 FAB，58×58dp 圓形，`primary` 底、白色 `+`，用
   `NavigationBar` 搭配一個獨立疊加的 `FloatingActionButton`（Compose 沒有內建
   「中央凸起 FAB 分頁」樣式，需要自己疊層，這是唯一需要手刻版面的地方）
3. **統計** — 路由 `/stats`，圖示用長條圖風格的 icon

選取態用 `NavigationBarItemDefaults` 的 `selectedIconColor`/`selectedTextColor`
設成 `primary`，未選取設成 fg3。**設定不進底部導覽**——從首頁右上角進入（見 §4.1）。

底部導覽在記帳頁（新增/編輯交易）**隱藏**——用 Compose Navigation 的
`currentBackStackEntryAsState()` 判斷目前路由決定要不要顯示 `NavigationBar`。

### §4 首頁

#### §4.1 標題區

左側：錢包名稱（`navTitle` 字級）+ 一個下拉圖示，整塊是可點的 `Row`（用
`Modifier.clickable`），點擊開啟錢包切換 `ModalBottomSheet`。
右側：「設定」文字鍵，`primary` 色，15sp，用 `TextButton`（不要用純 `Text` 加
`clickable`，`TextButton` 天然滿足 §1.2 的可點區域規則）。分類管理入口在設定頁內，
不在首頁標題列。

#### §4.2 預算卡

`Card`（Material 3），圓角 20dp，內距 22dp。由上而下：

1. 標籤（`label` 字級，fg2）：`budgetMode == WEEKLY` → 「本週還可以花」；
   `TOTAL` → 「總預算還剩」；`NONE` → 「本週支出」
2. 金額（`balance` 字級）。超支時整段文字改 `error` 色，並在金額右側加一個
   `error` 色的「已超支」標籤（不用 emoji 警示圖示）
3. 進度條（`LinearProgressIndicator`）：高 8dp、底色 track、填色 primary
   （超支時 error），寬度 = `usedPercent`（上限 100%）。`budgetMode == NONE`
   時不顯示
4. 一行兩端對齊（`caption` 字級，fg2）：左「已用 {usedText} / {budgetText}」、
   右「還有 {n} 天」
5. 上邊框 sep 的一行：左「日均可用」（`caption`，fg2）、右金額（15sp/W500，主色）

「還有 n 天」與「日均可用」需要對應網頁版的 `daysLeftInWeek` / `dailyAllowance`
兩個純函式（見 `TESTCASES.md` T7.1、T7.2，這兩個測案是網頁版 Phase 8 才新增的，
Android 版建議在 Phase 2（Budget domain）就一併做掉，不用像網頁版分兩個 phase）。

#### §4.3 交易列表

依週分組，每組：

- 組標題（兩端對齊）：左「本週 · 8/31–9/6」風格的人性化標籤（本週/上週/更早三種
  規則，日期格式一律 `M/D`，不輸出 ISO 格式）、右該組小計
- 群組容器：`Card` 圓角 16dp；列與列之間用 `HorizontalDivider`（最後一列無）

每列（最小高 44dp）：
- 左：38×38dp 圓角 12dp 的分類色塊（tint 底）+ emoji
- 中：分類名（`rowTitle`）/ 副標（`caption`，fg2）= 「M/D」或「M/D · 備註」，
  單行溢出用 `overflow = TextOverflow.Ellipsis`
- 右：金額（`rowAmount`），支出用主文字色前綴 `-`，收入用 income 色前綴 `+`
- **不顯示編輯 / 刪除文字鍵**

互動：
- 點整列 → 導向編輯交易頁
- 左滑 → 用 `SwipeToDismissBox`（Material 3 內建元件）露出刪除鍵，不要手刻手勢
- 刪除後用 `Snackbar` 顯示「已刪除 {分類} {金額}」，附「還原」動作鍵
  （`SnackbarResult.ActionPerformed`）
- 空狀態：置中 emoji 📝 + 「這個錢包還沒有交易」+「記第一筆」按鈕

### §5 記帳頁

全螢幕（`Scaffold` 不含底部導覽）。由上而下：

1. 標題列：左「取消」文字鍵、中支出/收入 `SegmentedButton`（Material 3 內建元件）、
   右留白對稱
2. 金額區（置中）：上方「{錢包名} · {幣別}」小字；下方大字金額，未輸入時顯示
   `NT$0` 且色為 fg3，有輸入時主色（收入用 income 色），即時千分位格式化
3. 分類網格：4 欄 `LazyVerticalGrid`，每格 50×50dp 圓角 16dp 的 tint 色塊 + emoji +
   11sp 名稱，選取態用 `Modifier.border` 加 2.5dp 的分類色邊框。**不要用
   `DropdownMenu` 或任何下拉選單**——這是網頁版特別強調要拿掉原生 `<select>`
   的地方，Android 版對應的坑是不要圖方便用 `ExposedDropdownMenuBox`
4. 日期 pill 列：今天／昨天／選日期（`DatePickerDialog`）三個選項；右側「+備註」
   pill，點擊展開單行 `TextField`
5. 數字鍵台（貼底）：3×4 `LazyVerticalGrid`，鍵序 `1-9 / 00 0 ⌫`。**這是自製鍵台，
   不是系統輸入法**——記帳頁全程 `TextField` 不應該取得真正的系統焦點去呼叫
   Android 內建鍵盤（可以用一個唯讀的 `Text` 顯示金額，不要用會呼叫系統鍵盤的
   `TextField`）
6. 主鍵：整寬按鈕，文案「記一筆」（編輯時「儲存」）。金額為 0 時 disabled

輸入規則：最多 8 位數字；前導 0 自動去除；`⌫` 逐字刪除（純函式邏輯跟網頁版
`TESTCASES.md` T7.3 完全相同，直接照搬成 Kotlin）。

成功後導回首頁並顯示 Snackbar「已記錄 {分類} {金額}」。

### §6 統計頁

1. 大標題「統計」（`titleLarge`）
2. 本週 / 本月 `SegmentedButton`（整寬）
3. 圓環卡：用 Compose `Canvas` 手畫圓環（`drawArc`），**不要引入圖表函式庫**
   （跟網頁版一樣的原則：手繪 SVG → 這裡對應手畫 Canvas）。每個分類一段弧，
   顏色 = 分類固定色。圓環中心疊一個獨立的 `Box` + `Text`（不是畫在 Canvas 裡的
   文字，Canvas 畫文字排版麻煩，疊 Compose `Text` 更好控制，這點跟網頁版用
   HTML 疊在 SVG 上是同樣的取巧邏輯）：上方「{本週|本月}支出」，下方期間總額
   （`donutTotal` 字級）。圖例列表：色塊 + emoji + 名稱，右側金額與百分比，
   下方 4dp 進度條
4. 趨勢卡：8 根柱狀圖同樣用 Canvas 手畫，當週柱用 primary 色，其餘用中性色；
   平均虛線用 `drawLine` 搭配 `PathEffect.dashPathEffect`；週別標籤用 Compose
   `Row` + `Text`（不是畫在 Canvas 裡），跟柱子對齊，當週標籤用 primary 色

### §7 錢包切換（`ModalBottomSheet`）

用 Compose Material 3 的 `ModalBottomSheet`，不要手刻 sheet 動畫或背景遮罩。

- 標題「切換錢包」（`caption`，fg2，置中）
- 每個未封存錢包一列：左「{名稱}」/「{幣別} · {該錢包當期餘額}」，右目前錢包
  顯示一個 primary 色的勾選圖示
- 末列「管理錢包…」（primary 色）→ 導向錢包管理頁
- 點列表項目切換錢包，切換後用 `Snackbar` 顯示「已切換到 {名稱}」，並自動關閉 sheet

切換錢包後預算卡、交易列表、統計、幣別符號必須全部跟著換——這件事在 MVVM
架構下應該自然發生：只要 `ViewModel` 的 `currentWalletId` 是一個 `StateFlow`，
所有依賴它的畫面用 `combine` 或 `flatMapLatest` 重新查詢，不需要手動通知每個畫面。

---

## §8 需要新增的 domain 函式

放 `domain/Budget.kt`，純函式、時間由參數注入，**不得**在 domain 層呼叫
`Clock.System.now()` 或任何系統時間 API：

```kotlin
/** 從 referenceDate 到本週結束（含當日）還有幾天。 */
fun daysLeftInWeek(
    weekStartDay: WeekStartDay,
    referenceDate: LocalDate,
): Int

/** 剩餘預算 ÷ 剩餘天數，向下取整到最小單位；剩餘 ≤ 0 時回傳 0。 */
fun dailyAllowance(
    remaining: Money,
    daysLeft: Int,
): Money
```

其餘數字（`usedPercent`、`isOverBudget`、分類彙總、週趨勢）比照網頁版 `budget.ts`
的函式簽章對應翻譯即可，不要在 UI 層重算。

---

## §9 驗收

以下任一項不成立就不算完成：

- 標題列不存在任何模擬連結樣式的純文字按鈕
- 交易列表列上不存在「編輯」「刪除」文字鍵
- 分類選擇不使用 `DropdownMenu`／`ExposedDropdownMenuBox`
- 分類色在不同期間、不同排序下保持一致
- 週分組標題不出現 `YYYY-MM-DD` 格式
- 趨勢圖的 8 個週別標籤在畫面上可見，且水平位置對齊各自的柱子
- 深色模式下每個畫面的文字對比 ≥ 4.5:1
- 所有可點目標 ≥ 48×48 dp
- `./gradlew test`（domain 單元測試）與 Maestro flow 全綠
