// 在 `clearState` 之後、`launchApp` 之前刻意等一段時間，繞開 Android
// ActivityManager 的一個 race——這是 Phase 6 在 CI 上連續失敗 4 次才從
// device logcat 裡挖出來的真正原因（見 TASKS.md TD-3）。
//
// 症狀：`launchApp: { clearState: true }` 之後的第一個 assertVisible 會在
// 約 18 秒（Maestro 預設等待逾時）後失敗，截圖是一片空白、只剩狀態列跟導覽列，
// view hierarchy 裡**完全找不到 app 的視窗**（只剩 com.android.systemui）。
// 每次踩到的是不同一支 flow，本機 arm64 emulator 從來不會踩到。
//
// 真正原因（CI logcat 逐行對照）：
//   13:46:43.644 Force stopping ...: clear data        <- pm clear（clearState）
//   13:46:43.647 Force removing ActivityRecord{... t9} <- 舊的 Task #9 被拆掉
//   13:46:44.331 START ... MainActivity ... taskId=10  <- Maestro 立刻重新啟動（+0.69s）
//   13:46:44.347 Start proc 2998 ... for next-top-activity
//   13:46:44.796 Destroy timeout of remove-task, attempt to kill Task{#9}  <- 舊 task 的
//                                                       destroy timeout 這時才觸發（+1.15s）
//   13:46:44.800 Killing 2998 (adj -10000): remove task <- 卻把「剛啟動的新 process」殺掉
//   13:46:44.852 Exception thrown during bind of ProcessRecord{2998}
//
// `pm clear` 拆掉舊 task 時會排一個約 1.15 秒後才觸發的 destroy timeout；如果在這個
// 視窗內就重新啟動 App，timeout 觸發時 AMS 會把「這個 package 目前的 process」解讀成
// 要清掉的對象，於是把**剛啟動的那個新 process** 殺掉。App 就這樣在啟動後立刻死掉，
// 只留下一個空的視窗（所以截圖是主題背景色的空白畫面），Maestro 接著白等 17 秒逾時。
//
// 為什麼本機不會踩到：本機 arm64 emulator 拆 task 拆得快，重新啟動時視窗早就關了。
// 為什麼 CI 會踩到：x86_64 emulator 慢，重新啟動剛好落在視窗內——是純粹的時間競態，
// 所以每次中獎的 flow 都不一樣。
//
// **注意：把斷言的等待時間拉長沒有用**——process 已經被殺掉了，等再久也不會自己回來。
// 唯一的解法是讓 destroy timeout 在「還沒有新 process 存在」的時候就先觸發完畢，
// 也就是 clearState 之後先等超過那個視窗（實測 1.15 秒）再啟動。這裡等 3 秒，
// 留約 2.6 倍的餘裕給更慢的 runner。
//
// 為什麼用忙等而不是 sleep：Maestro 的 JS 環境（GraalJS）沒有開 host access，
// `Java.type('java.lang.Thread').sleep()` 會直接丟例外（實測過），也沒有 setTimeout，
// 所以只能忙等。3 秒 × 8 支 flow 的成本可以接受，而且這段時間 AMS 那個 timeout
// 本來就是時鐘驅動的，不會因為 host 少一顆核心就不觸發。
var SETTLE_MS = 3000;

var end = Date.now() + SETTLE_MS;
while (Date.now() < end) {
  // 忙等，理由見上面
}
