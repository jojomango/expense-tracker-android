// E2E-10 統計：本週/本月分類支出佔比與近 8 週趨勢。
// 跟 e2e5-dates.js 同樣的理由（emulator 沒有 root，鎖不住系統時鐘）：不照抄
// TESTCASES.md 的 2026-08-11 固定日期，改成用相對日期。
//
// 原本的設計是「這週一」/「上週一」，但這會在「今天剛好是這個月的第一週」時
// 整個垮掉：這週一可能落在上個月（例如今天 9/5 週六，這週一是 8/31），這樣
// Month.rangeOf(today()) 用的是「今天所在月份」查詢，會把兩筆「這週」的交易
// 整個排除在外（本月變成 $0）。這是實測踩到的真實案例，不是假設。
//
// 修正後改成：
// - 「這週」交易一律用「今天」本身（保證同週、同月，不需要任何 DatePicker
//   月份導覽）。
// - 「同月但不同週」交易改用「今天 ±7 天」搜尋：往前 7 天或往後 7 天，兩者
//   之中至少有一個一定跟今天同月（因為任何月份長度都 ≥28 天，數學上不可能
//   兩邊都跨月），優先用往前（比較符合「上一週」的語意），跨月才退而求其次
//   用往後。這樣月份導覽次數永遠是 0，且 100% 保證monthly assertion 不會因
//   為今天是幾號而變動。
function pad(n) {
  return n < 10 ? "0" + n : "" + n;
}

var monthNames = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];
var weekdayNames = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

// DatePicker 的今天格子無障礙文字是「Today, Saturday, September 5, 2026」這種
// 多一個「Today, 」前綴的格式，跟其他日期的格式不一樣。
function formatDayCell(d, referenceToday) {
  var base = weekdayNames[d.getDay()] + ", " + monthNames[d.getMonth()] + " " + d.getDate() + ", " + d.getFullYear();
  return isSameDay(d, referenceToday) ? "Today, " + base : base;
}

function monthsBack(from, to) {
  return (from.getFullYear() - to.getFullYear()) * 12 + (from.getMonth() - to.getMonth());
}

function mondayOf(d) {
  var dow = d.getDay();
  var diffToMonday = dow === 0 ? -6 : 1 - dow;
  var m = new Date(d);
  m.setDate(d.getDate() + diffToMonday);
  return m;
}

function sameMonth(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth();
}

function shortLabel(d) {
  return (d.getMonth() + 1) + "/" + d.getDate();
}

var today = new Date();
var thisMonday = mondayOf(today);

var backward = new Date(today);
backward.setDate(today.getDate() - 7);
var forward = new Date(today);
forward.setDate(today.getDate() + 7);
// 往前跟往後至少有一個一定跟 today 同月（任何月份長度都 ≥28 天），優先用往前。
var otherDate = sameMonth(backward, today) ? backward : forward;

output.todayLabel = formatDayCell(today, today);
output.otherLabel = formatDayCell(otherDate, today);
output.monthsBackToday = String(monthsBack(today, today));
output.monthsBackOther = String(monthsBack(today, otherDate));
output.thisMondayShort = shortLabel(thisMonday);
