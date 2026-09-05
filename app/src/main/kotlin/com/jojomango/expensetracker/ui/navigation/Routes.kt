package com.jojomango.expensetracker.ui.navigation

/** UI-SPEC.md §3.1：三個底部導覽路由 + 記帳頁（隱藏底部導覽）+ 設定（不進底部導覽）。 */
object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ADD_TRANSACTION = "transaction/new"
    const val EDIT_TRANSACTION_PATTERN = "transaction/edit/{transactionId}"
    const val WALLET_MANAGEMENT = "wallet/manage"
    const val WALLET_NEW = "wallet/new"
    const val WALLET_EDIT_PATTERN = "wallet/edit/{walletId}"

    fun editTransaction(transactionId: String) = "transaction/edit/$transactionId"

    fun editWallet(walletId: String) = "wallet/edit/$walletId"
}

/** 底部導覽會隱藏的路由——記帳頁全螢幕，見 UI-SPEC.md §3.1；錢包管理/編輯也是
 * 全螢幕的表單流程，不是底部導覽的目的地。 */
fun isBottomNavHiddenRoute(route: String?): Boolean =
    route == Routes.ADD_TRANSACTION ||
        route == Routes.WALLET_MANAGEMENT ||
        route == Routes.WALLET_NEW ||
        (route?.startsWith("transaction/edit/") == true) ||
        (route?.startsWith("wallet/edit/") == true)
