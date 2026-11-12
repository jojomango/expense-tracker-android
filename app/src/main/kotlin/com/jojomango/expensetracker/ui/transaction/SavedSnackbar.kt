package com.jojomango.expensetracker.ui.transaction

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryNames
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.categoryDisplayName
import kotlinx.coroutines.launch

/** 回首頁時透過 back stack 的 SavedStateHandle 傳遞訊息用的 key。 */
const val SAVED_TRANSACTION_MESSAGE_KEY = "saved_transaction_message"

/** UI-SPEC.md §5「已記錄 {分類} {金額}」；金額用這筆交易的幣別（§15.2）。 */
internal fun savedTransactionMessage(
    context: Context,
    transaction: Transaction,
    categories: List<Category>,
    names: CategoryNames,
): String =
    context.getString(
        R.string.transaction_saved,
        categoryDisplayName(transaction.categoryId, categories, names),
        Money.of(transaction.amount, transaction.currency).format(),
    )

/**
 * 先呼叫 [onShown] 再顯示：訊息存在 SavedStateHandle，若等 Snackbar 消失才清掉，
 * 中途切到別的分頁再回來會重播一次。清掉會換掉 LaunchedEffect 的 key，
 * 所以 Snackbar 要放在畫面層級的 scope 裡跑，不然一清就被取消。
 */
@Composable
internal fun SavedTransactionSnackbar(
    message: String?,
    hostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (message != null) {
            onShown()
            scope.launch { hostState.showSnackbar(message) }
        }
    }
}
