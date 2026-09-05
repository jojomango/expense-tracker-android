// E2E-5 週起始日設定：算出「這週一」跟「這週一的前一天（週日）」，讓 flow
// 不用寫死日曆日期也能保證同樣的相對位置關係（週一屬於「本週」、週日屬於
// 「上週」——跟 TESTCASES.md 給的 2026-08-09/08-10 例子結構完全一樣，
// 差別只在於這裡用「執行當下的今天」算，不管哪一天跑都一樣正確）。
function pad(n) {
  return n < 10 ? "0" + n : "" + n;
}

var today = new Date();
var dow = today.getDay(); // 0=Sun..6=Sat
var diffToMonday = dow === 0 ? -6 : 1 - dow;
var monday = new Date(today);
monday.setDate(today.getDate() + diffToMonday);
var sundayBefore = new Date(monday);
sundayBefore.setDate(monday.getDate() - 1);

var monthNames = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];
var weekdayNames = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

function formatDayCell(d) {
  return weekdayNames[d.getDay()] + ", " + monthNames[d.getMonth()] + " " + d.getDate() + ", " + d.getFullYear();
}

function monthsBack(from, to) {
  return (from.getFullYear() - to.getFullYear()) * 12 + (from.getMonth() - to.getMonth());
}

output.dateALabel = formatDayCell(sundayBefore);
output.dateBLabel = formatDayCell(monday);
output.monthsBackA = String(monthsBack(today, sundayBefore));
output.monthsBackB = String(monthsBack(today, monday));
