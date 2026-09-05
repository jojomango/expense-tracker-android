package com.jojomango.expensetracker.domain

import kotlin.math.round

/**
 * 金額，以幣別最小單位的整數（[amount]）儲存——見 SPEC.md P2。
 * 禁止用 Float/Double 表示金額；僅 [percentOf] 回傳 Double，且僅供顯示用途。
 */
@ConsistentCopyVisibility
data class Money internal constructor(
    val amount: Long,
    val currency: CurrencyInfo,
) {
    companion object {
        fun of(
            amount: Long,
            code: String,
            registry: CurrencyRegistry = CurrencyRegistry(),
        ): Money = Money(amount, registry.resolve(code))

        /**
         * 解析使用者輸入字串（容許千分位逗號、小數點），依幣別的小數位數驗證。
         * 超過小數位數、或格式不合法一律拋 [IllegalArgumentException]。
         */
        fun parse(
            input: String,
            code: String,
            registry: CurrencyRegistry = CurrencyRegistry(),
        ): Money {
            val currencyInfo = registry.resolve(code)
            val cleaned = input.replace(",", "").trim()
            val pattern = Regex("""^-?\d+(\.\d+)?$""")
            require(pattern.matches(cleaned)) { "Invalid money input: \"$input\"" }

            val negative = cleaned.startsWith("-")
            val unsigned = cleaned.removePrefix("-")
            val dotIndex = unsigned.indexOf('.')
            val integerPart = if (dotIndex >= 0) unsigned.substring(0, dotIndex) else unsigned
            val fractionalPart = if (dotIndex >= 0) unsigned.substring(dotIndex + 1) else ""
            require(fractionalPart.length <= currencyInfo.decimalDigits) {
                "Too many decimal places for ${currencyInfo.code}: \"$input\""
            }

            val paddedFractional = fractionalPart.padEnd(currencyInfo.decimalDigits, '0')
            val magnitude = (integerPart + paddedFractional).toLong()
            val signedAmount = if (negative) -magnitude else magnitude
            return Money(signedAmount, currencyInfo)
        }

        /**
         * 加總一組 [Money]；空清單時仍需明確指定 [code] 才知道結果的幣別。
         */
        fun sum(
            items: List<Money>,
            code: String,
            registry: CurrencyRegistry = CurrencyRegistry(),
        ): Money {
            val currencyInfo = registry.resolve(code)
            val total =
                items.fold(0L) { acc, money ->
                    require(money.currency.code == code) {
                        "Currency mismatch in sum: expected $code, got ${money.currency.code}"
                    }
                    acc + money.amount
                }
            return Money(total, currencyInfo)
        }
    }

    fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount + other.amount, currency)
    }

    fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount - other.amount, currency)
    }

    /**
     * `this` 占 [denominator] 的百分比，四捨五入到小數點後兩位，僅供顯示用途。
     * 分母為 0 時回傳 `0.0`（不得為 NaN/Infinity）。
     */
    fun percentOf(denominator: Money): Double {
        requireSameCurrency(denominator)
        if (denominator.amount == 0L) return 0.0
        val raw = amount.toDouble() / denominator.amount.toDouble() * 100.0
        return round(raw * 100) / 100.0
    }

    /**
     * 格式化為使用者可見字串，例如 `NT$100.50`、`¥1,000`、`-NT$5.00`。
     * 千分位分組手刻實作，不依賴平台 locale（見 SPEC.md P1 的可攜性精神）。
     */
    fun format(): String {
        val sign = if (amount < 0) "-" else ""
        val absAmount = kotlin.math.abs(amount)
        val decimalDigits = currency.decimalDigits
        val divisor = pow10(decimalDigits)
        val integerPart = absAmount / divisor
        val fractionalPart = absAmount % divisor

        val grouped = groupThousands(integerPart)
        return if (decimalDigits > 0) {
            val fractionalString = fractionalPart.toString().padStart(decimalDigits, '0')
            "$sign${currency.symbol}$grouped.$fractionalString"
        } else {
            "$sign${currency.symbol}$grouped"
        }
    }

    override fun toString(): String = format()

    private fun requireSameCurrency(other: Money) {
        require(currency.code == other.currency.code) {
            "Currency mismatch: ${currency.code} vs ${other.currency.code}"
        }
    }
}

/** 10 的 [exponent] 次方——用來在「使用者輸入的整數位數」與「幣別最小單位」之間換算。 */
fun pow10(exponent: Int): Long {
    var result = 1L
    repeat(exponent) { result *= 10 }
    return result
}

private fun groupThousands(value: Long): String {
    val digits = value.toString()
    val builder = StringBuilder()
    for ((index, char) in digits.withIndex()) {
        if (index > 0 && (digits.length - index) % 3 == 0) builder.append(',')
        builder.append(char)
    }
    return builder.toString()
}
