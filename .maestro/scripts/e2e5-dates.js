// E2E-5 週起始日設定。TESTCASES.md 的例子是「今天週二 08-11、週日 08-09 一筆 1000、
// 週一 08-10 一筆 500」：週一起始時只算週一那筆（餘額 2500），改成週日起始後兩筆都算
// （餘額 1500）。這台 emulator 沒有 root 鎖不住系統時鐘，所以用執行當下的「今天」算出
// 相對位置一樣的兩個日期：
//   A：週日起始的「本週」有、週一起始的「本週」沒有（1000 那筆）
//   B：兩種起始日的「本週」都有（500 那筆）
//
// 平日（週一～週六）：A = 這週日起始那週的週日（= 這週一的前一天），B = 這週一。
// **今天是週日時，過去的日期不可能滿足 A**：週日起始的本週從今天才開始，而今天又是週一
// 起始那週的最後一天，所以 A 只能用明天（週一，SPEC.md §3.2 允許未來日期），B 用今天。
// （Phase 8 剛好在週日跑到才發現原本的寫法在週日一定失敗。）
var weekdayNames = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
var monthNames = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

function addDays(d, n) {
  var r = new Date(d);
  r.setDate(d.getDate() + n);
  return r;
}

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

// DatePicker 的今天那一格無障礙文字多一個「Today, 」前綴（E2E-10 踩過）。
function formatDayCell(d, today) {
  var base = weekdayNames[d.getDay()] + ", " + monthNames[d.getMonth()] + " " + d.getDate() + ", " + d.getFullYear();
  return isSameDay(d, today) ? "Today, " + base : base;
}

// 正數 = 要按幾次「上個月」，負數 = 要按幾次「下個月」。DatePicker 一打開停在今天的月份。
function monthOffset(from, to) {
  return (from.getFullYear() - to.getFullYear()) * 12 + (from.getMonth() - to.getMonth());
}

var today = new Date();
var dow = today.getDay(); // 0 = 週日
var a, b;
if (dow === 0) {
  a = addDays(today, 1);
  b = today;
} else {
  b = addDays(today, 1 - dow); // 這週一
  a = addDays(b, -1); // 這週一的前一天（週日）
}

var offA = monthOffset(today, a);
var offB = monthOffset(today, b);
output.dateALabel = formatDayCell(a, today);
output.dateBLabel = formatDayCell(b, today);
output.monthsBackA = String(Math.max(offA, 0));
output.monthsForwardA = String(Math.max(-offA, 0));
output.monthsBackB = String(Math.max(offB, 0));
output.monthsForwardB = String(Math.max(-offB, 0));
