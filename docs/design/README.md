# 設計決策（UX）

> 這個資料夾是 **UI／UX 改版的交接點**：設計討論在 Claude chat 進行，實作在 Claude Code 進行，
> 兩邊都讀這裡的檔案掌握進度。repo 是唯一的真相來源。

## 總表

下一個新決策的編號：**UX-008**（每新增一份就把這行加 1）

| 編號 | 主題 | 對應截圖 | 狀態 | 合約 PR | 實作 PR | 更新日期 |
|---|---|---|---|---|---|---|
| [UX-001](decisions/UX-001-add-category-grid.md) | 記帳頁分類網格一次露出兩列 | 04、05、06、10、28 | 已完成 | [#29](https://github.com/jojomango/expense-tracker-android/pull/29) | [#30](https://github.com/jojomango/expense-tracker-android/pull/30) | 2026-10-27 |
| [UX-002](decisions/UX-002-add-note-keyboard.md) | 編輯備註時數字鍵收起 | 06、10、28 | 已完成 | [#29](https://github.com/jojomango/expense-tracker-android/pull/29) | [#32](https://github.com/jojomango/expense-tracker-android/pull/32) | 2026-10-27 |
| [UX-003](decisions/UX-003-category-display-order.md) | 分類顯示順序照預設順序 | 04、05、19、28 | 已完成 | [#29](https://github.com/jojomango/expense-tracker-android/pull/29) | [#30](https://github.com/jojomango/expense-tracker-android/pull/30) | 2026-10-27 |
| [UX-004](decisions/UX-004-add-date-icon.md) | 「選日期」改為日曆圖示 | 04、05、06、10、27、28 | 已決定 | | | 2026-10-28 |
| [UX-005](decisions/UX-005-add-note-category-row.md) | 備註聚焦時分類單列／矮手機摘要列 | 06、10、28 | 已決定 | | | 2026-10-28 |
| [UX-006](decisions/UX-006-home-list-day-groups.md) | 首頁日小標題、預算卡隨列表捲走、年份分隔、底部留白 | 08、09、24、25、27 | 已決定 | | | 2026-10-28 |
| [UX-007](decisions/UX-007-row-delete-talkback.md) | 交易列 TalkBack 刪除動作 | 08、09 | 已決定 | | | 2026-10-28 |

**待設計討論**（實作時人類先決定、要在下一次設計討論確認的項目）

| 來源 | 內容 | 日期 |
|---|---|---|
| UX-005（#39） | 360×700 摘要列下方約 50dp 空白：先照實作上線，人類實測後決定是否調整 | 2026-10-29 |
| UX-005（#39） | 640dp 高、備註展開但鍵盤未開時分類網格被擠到不可見（UX-001 既有行為）：pending，實測後決定 | 2026-10-29 |
| UX-005（#39） | 摘要列以 clearAndSetSemantics 合成一句給 TalkBack：人類實測後看是否需調整 | 2026-10-29 |

**狀態的意思**

| 狀態 | 意思 |
|---|---|
| 提案 | 設計討論提出，已存進 repo，等人類確認 |
| 已決定 | 人類確認要做，等實作那邊排程 |
| 合約已更新 | `UI-SPEC.md`（必要時 `TESTCASES.md`）已經照決策修改並 merge |
| 實作中 | 實作 PR 已開，尚未 merge |
| 已完成 | 實作 PR 已 merge，操作導覽截圖已重拍 |
| 擱置 | 暫時不做，檔案裡寫明原因 |
| 取消 | 決定不做，檔案裡寫明原因 |

只有實作那邊（Claude Code）會改總表與狀態；設計那邊要調整已經「合約已更新」或「已完成」的決策時，開一個**新編號**，在新檔案裡寫「取代 UX-00N」。

## 流程

```
設計（Claude chat）         人類                      實作（Claude Code）
1. 讀這份總表與 UI-SPEC
2. 產出決策單（artifact
   或 Markdown 程式碼區塊） → 3. 把連結或內容轉交 →  4. 存成 decisions/UX-NNN-*.md，狀態「提案」
                             5. 確認要不要做       →  6. 狀態改「已決定」
                                                       7. 開只改 UI-SPEC 的 draft PR
                             8. review、merge      ←
                                                       9. 狀態改「合約已更新」，照 TDD 實作、跑 E2E
                             10. review、merge     ←   11. 狀態改「已完成」，重拍操作導覽截圖
```

- 設計決策**不會跳過合約**：一律先改 `UI-SPEC.md`，由人類 merge 簽署，再寫程式（CLAUDE.md 禁令 1）
- 決策跟現有合約衝突時（例如 TESTCASES 寫死的行為），實作那邊會先停下來問人類，不會自己選邊
- 參考資料：
  - 介面規格：[`UI-SPEC.md`](../../UI-SPEC.md)；產品規格：[`SPEC.md`](../../SPEC.md)；實作限制：[`CLAUDE.md`](../../CLAUDE.md)
  - 最新畫面：操作導覽截圖 https://claude.ai/artifact/6e96D2c3jSTuhAkssNMQxX （每張截圖有編號，討論時用編號指畫面；只有被分享的人打得開）
  - 決策單格式：[`TEMPLATE.md`](TEMPLATE.md)
  - 設計畫布（UX-004～007 的模擬圖）：https://claude.ai/artifact/Tw2d6cpLrfZksTkRW1ndNF

## 給設計討論用的 prompt

在 Claude chat 開新的設計對話時，先貼上這段：

```text
你是「記帳本」Android App 的 UI／UX 設計夥伴。repo：jojomango/expense-tracker-android。

開始前請先讀：
1. docs/design/README.md —— 設計決策總表：每一項的編號、狀態、對應 PR，以及下一個新決策的編號
2. UI-SPEC.md —— 目前的介面規格（色票 token、字級、元件、各畫面）
3. SPEC.md 與 CLAUDE.md —— 產品與實作限制
4. 最新的操作導覽截圖：https://claude.ai/artifact/6e96D2c3jSTuhAkssNMQxX（截圖有編號，討論時用編號指畫面）

設計必須符合這些限制：
- Material 3 + Jetpack Compose；不新增任何套件
- 所有文字都要有繁中與英文兩個版本；繁中是主要語言
- 可點擊區域至少 48dp；深色模式的文字對比 ≥ 4.5:1
- 顏色、字級盡量沿用 UI-SPEC 既有的 token；要新增 token 請明講
- 不做廣告、不在首頁／記帳／統計頁放贊助入口（SPEC §3.8）
- 狀態已經是「合約已更新」或「已完成」的決策，請不要直接改寫；要調整就開一個新編號，寫明「取代 UX-00N」

你無法直接寫入 repo，請用以下方式交付決策：每做出一個決策，照 docs/design/TEMPLATE.md 的格式
產出一份完整的 Markdown 決策單，發布成一個 artifact（或放在一個程式碼區塊裡），
並在開頭寫上建議檔名：docs/design/decisions/UX-<編號>-<英文短名>.md，狀態填「提案」。
我會把連結或內容轉交給實作那邊，由它存進 repo、更新 docs/design/README.md 的總表。
一份決策單只放一個主題。還沒有結論的問題寫進「待決定」段落，不要替我做決定。
```
