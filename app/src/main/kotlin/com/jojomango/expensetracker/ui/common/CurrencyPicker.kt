@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.common

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.LocaleList
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.Currencies
import java.util.Locale

const val CURRENCY_FIELD_TAG = "currency_field"

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val nameZh: String,
    val nameEn: String,
)

/** UI-SPEC.md §14.2：代碼開頭、中文名稱包含、英文任一單字開頭；代碼符合的排前面。 */
fun filterCurrencies(
    options: List<CurrencyOption>,
    query: String,
): List<CurrencyOption> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return options
    val byCode = options.filter { it.code.lowercase().startsWith(q) }
    val byName =
        options.filter { option ->
            option !in byCode &&
                (
                    option.nameZh.contains(q) ||
                        option.nameEn
                            .lowercase()
                            .split(' ')
                            .any { it.startsWith(q) }
                )
        }
    return byCode + byName
}

private val currencyNameRes: Map<String, Int> =
    mapOf(
        "TWD" to R.string.currency_name_twd,
        "JPY" to R.string.currency_name_jpy,
        "USD" to R.string.currency_name_usd,
        "EUR" to R.string.currency_name_eur,
        "KRW" to R.string.currency_name_krw,
        "CNY" to R.string.currency_name_cny,
        "HKD" to R.string.currency_name_hkd,
        "GBP" to R.string.currency_name_gbp,
        "AUD" to R.string.currency_name_aud,
        "SGD" to R.string.currency_name_sgd,
        "THB" to R.string.currency_name_thb,
        "VND" to R.string.currency_name_vnd,
        "MYR" to R.string.currency_name_myr,
        "PHP" to R.string.currency_name_php,
        "IDR" to R.string.currency_name_idr,
        "INR" to R.string.currency_name_inr,
        "CAD" to R.string.currency_name_cad,
        "CHF" to R.string.currency_name_chf,
        "NZD" to R.string.currency_name_nzd,
        "MOP" to R.string.currency_name_mop,
    )

// 搜尋要同時比對中英文名稱（不論介面語言），所以兩種語言的資源都要讀
private fun Context.inLocale(tag: String): Context =
    createConfigurationContext(Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) })

private fun currencyOptions(context: Context): List<CurrencyOption> {
    val zh = context.inLocale("zh-TW")
    val en = context.inLocale("en")
    return Currencies.builtIn.values.map { info ->
        val res = currencyNameRes.getValue(info.code)
        CurrencyOption(info.code, info.symbol, zh.getString(res), en.getString(res))
    }
}

/** 幣別代碼可以直接打英文；鍵盤先給英文但不鎖死，想打中文名稱搜尋時仍可切換。 */
private val CurrencySearchKeyboardOptions = KeyboardOptions(autoCorrectEnabled = false, hintLocales = LocaleList("en"))

/** UI-SPEC.md §14：只能選內建幣別的可搜尋下拉選單；收起時欄位顯示目前選取的幣別。 */
@Composable
fun CurrencyPickerField(
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val options = remember(configuration) { currencyOptions(context) }
    val label: (CurrencyOption) -> String = { option ->
        context.getString(R.string.currency_option, context.getString(currencyNameRes.getValue(option.code)), option.code, option.symbol)
    }
    var expanded by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val close = {
        expanded = false
        query = ""
    }
    val current = options.firstOrNull { it.code == selected }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = {
            if (enabled) {
                if (it) {
                    expanded = true
                } else {
                    close()
                }
            }
        },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = if (expanded) query else current?.let(label) ?: selected,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(stringResource(R.string.wallet_currency_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = CurrencySearchKeyboardOptions,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled)
                    .onFocusChanged { if (!it.isFocused && expanded) close() }
                    .testTag(CURRENCY_FIELD_TAG),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = close) {
            val matches = filterCurrencies(options, query)
            if (matches.isEmpty()) {
                DropdownMenuItem(text = { Text(stringResource(R.string.currency_no_match)) }, onClick = {}, enabled = false)
            }
            matches.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onSelect(option.code)
                        close()
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
