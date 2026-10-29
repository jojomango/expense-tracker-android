# UX-007：首頁交易列補上 TalkBack「刪除」自訂動作（刪除仍只有左滑，不加編輯頁刪除鈕）

| 欄位 | 內容 |
|---|---|
| 狀態 | 實作中（已 merge，待重拍截圖） |
| 對應截圖 | 08、09（交易列與左滑刪除） |
| 要改的 UI-SPEC 章節 | §4.3 交易列表「互動」段（新增一條無障礙動作） |
| 取代 | 無 |
| 合約 PR | （實作那邊填） |
| 實作 PR | （實作那邊填） |

這一項來自首頁列表審查的項目 3（刪除的可發現性）。

## 現況問題

UI-SPEC §4.3 規定刪除只有一條路：左滑列露出刪除鍵。`TransactionRow` 沒有任何 `customActions` 語意，編輯交易頁也沒有刪除鈕。

結果：使用 TalkBack（Android 螢幕閱讀器）的人無法刪除交易，因為左滑手勢在 TalkBack 開啟時不會觸發 `SwipeToDismissBox`，列上也沒有可替代的動作。

## 決定

1. 每個交易列加一個 TalkBack 自訂動作：「刪除」，用 `Modifier.semantics { customActions = listOf(CustomAccessibilityAction(label) { ... }) }`。
2. 動作的行為與左滑刪除完全相同：呼叫同一個 `onDelete()`，之後顯示同一個「已刪除 {分類} {金額}」Snackbar 與「還原」動作鍵。不另做一條刪除邏輯。
3. 動作名稱使用既有字串 `common_delete`（「刪除」／Delete），不新增字串。
4. 其他刪除方式不變：一般使用者仍是左滑；編輯頁不加刪除鈕。

## 行為改變

資料與刪除邏輯不變，只新增一個進入點。`TESTCASES.md` 需要補一個測案：開啟 `customActions` 語意後，觸發「刪除」會移除該筆交易並顯示 Snackbar。若 E2E 不易驅動 TalkBack，可用 Compose 測試的 `performCustomAccessibilityActionWithLabel`（或對應的語意動作 API）代替。

## 文字（繁中／英文）

| 位置 | 繁中 | English |
|---|---|---|
| 交易列的 TalkBack 自訂動作名稱 | 刪除（既有 `common_delete`） | Delete（既有） |

沒有新增字串。

## 無障礙

- TalkBack 使用者選取交易列後，從動作選單選「刪除」即可刪除；刪除後 Snackbar 會被朗讀，使用者可以選「還原」。
- 列的點擊（進入編輯）、最小高度 44dp、色塊與文字內容都不變。
- 這一條讓整個首頁列表達到「所有功能都能不用手勢完成」。

## 不在這次範圍

- 一般使用者怎麼發現「可以左滑」：人類決定不處理（見下方人類決定），維持 UI-SPEC §4.3 現況。
- 編輯頁新增刪除鈕：已評估，人類決定不加。
- 左滑刪除後出現空白列（疑似 `rememberSwipeToDismissBoxState` 未以交易 id 為 key）：這是 bug，不是設計決定，請另行處理；修好前後都不影響本單。

## 人類決定（2026-10-28）

- 編輯頁不加刪除鈕，只補 TalkBack 自訂動作。左滑維持唯一的視覺刪除方式，不另外做可發現性提示。

## 待決定

無。
