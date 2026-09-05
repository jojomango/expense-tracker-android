package com.jojomango.expensetracker.ui.navigation

/** UI-SPEC.md §3.1：三個底部導覽路由 + 記帳頁（隱藏底部導覽）+ 設定（不進底部導覽）。 */
object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ADD_TRANSACTION = "transaction/new"
    const val EDIT_TRANSACTION_PATTERN = "transaction/edit/{transactionId}"

    fun editTransaction(transactionId: String) = "transaction/edit/$transactionId"
}

/** 底部導覽會隱藏的路由——記帳頁全螢幕，見 UI-SPEC.md §3.1。 */
fun isBottomNavHiddenRoute(route: String?): Boolean = route == Routes.ADD_TRANSACTION || (route?.startsWith("transaction/edit/") == true)
