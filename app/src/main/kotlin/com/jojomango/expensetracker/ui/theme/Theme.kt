package com.jojomango.expensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jojomango.expensetracker.domain.Theme

/**
 * UI-SPEC.md §2.1 沒有對應 Material 3 `ColorScheme` 欄位的自訂 token
 * （track/barBg/sheet/keypad/key/income/fg3），不要硬塞進不相關的 `ColorScheme`
 * 欄位（例如把 track 塞進 secondary）。
 */
data class AppExtraColors(
    val fg3: Color,
    val track: Color,
    val barBg: Color,
    val sheet: Color,
    val keypad: Color,
    val key: Color,
    val income: Color,
    /** UI-SPEC.md §2.2 分類色塊的淡色底透明度：light 12%、dark 18%。 */
    val categoryTintAlpha: Float,
)

val LocalAppExtraColors =
    staticCompositionLocalOf {
        AppExtraColors(
            fg3 = LightColors.fg3,
            track = LightColors.track,
            barBg = LightColors.barBg,
            sheet = LightColors.sheet,
            keypad = LightColors.keypad,
            key = LightColors.key,
            income = LightColors.income,
            categoryTintAlpha = LIGHT_TINT_ALPHA,
        )
    }

/** SPEC.md §3.5 的 `theme` 設定：`SYSTEM` 才跟著系統，其餘兩種是使用者明確覆寫。 */
fun Theme.isDark(systemDark: Boolean): Boolean =
    when (this) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> systemDark
    }

// 選取態的底色（FilterChip、SegmentedButton、底部導覽的選取指示）用 accent 的淡色底，
// 透明度跟 UI-SPEC.md §2.2 分類色 tint 同一組數值，視覺語言一致。
private const val LIGHT_TINT_ALPHA = 0.12f
private const val DARK_TINT_ALPHA = 0.18f

/**
 * 深色模式主按鈕上的文字色。UI-SPEC.md §2.1 沒有規定 onPrimary；白字壓在 dark accent
 * `#D9673F` 上只有 3.52:1，達不到 §9「深色模式文字對比 ≥ 4.5:1」。改用淺色模式的主文字色
 * `#111114`（5.35:1），重用既有 token，不另外發明新顏色。FAB 的「+」是圖示不是文字，
 * §3.1 明訂白色，在 `MainActivity` 另外指定。
 */
private val DarkOnAccent = LightColors.onSurface

private fun lightScheme(): ColorScheme {
    val tint = LightColors.accent.copy(alpha = LIGHT_TINT_ALPHA)
    return lightColorScheme(
        primary = LightColors.accent,
        onPrimary = Color.White,
        primaryContainer = tint,
        onPrimaryContainer = LightColors.onSurface,
        secondary = LightColors.accent,
        onSecondary = Color.White,
        secondaryContainer = tint,
        onSecondaryContainer = LightColors.onSurface,
        tertiary = LightColors.income,
        onTertiary = Color.White,
        background = LightColors.background,
        onBackground = LightColors.onSurface,
        surface = LightColors.surface,
        onSurface = LightColors.onSurface,
        surfaceVariant = LightColors.track,
        onSurfaceVariant = LightColors.onSurfaceVariant,
        surfaceTint = LightColors.surface,
        inverseSurface = LightColors.onSurface,
        inverseOnSurface = LightColors.surface,
        inversePrimary = DarkColors.accent,
        error = LightColors.danger,
        onError = Color.White,
        errorContainer = LightColors.danger.copy(alpha = LIGHT_TINT_ALPHA),
        onErrorContainer = LightColors.onSurface,
        outline = LightColors.onSurfaceVariant,
        outlineVariant = LightColors.sep,
        surfaceBright = LightColors.surface,
        surfaceDim = LightColors.background,
        surfaceContainerLowest = LightColors.surface,
        surfaceContainerLow = LightColors.surface,
        surfaceContainer = LightColors.surface,
        surfaceContainerHigh = LightColors.surface,
        surfaceContainerHighest = LightColors.surface,
    )
}

private fun darkScheme(): ColorScheme {
    val tint = DarkColors.accent.copy(alpha = DARK_TINT_ALPHA)
    return darkColorScheme(
        primary = DarkColors.accent,
        onPrimary = DarkOnAccent,
        primaryContainer = tint,
        onPrimaryContainer = DarkColors.onSurface,
        secondary = DarkColors.accent,
        onSecondary = DarkOnAccent,
        secondaryContainer = tint,
        onSecondaryContainer = DarkColors.onSurface,
        tertiary = DarkColors.income,
        onTertiary = DarkOnAccent,
        background = DarkColors.background,
        onBackground = DarkColors.onSurface,
        surface = DarkColors.surface,
        onSurface = DarkColors.onSurface,
        surfaceVariant = DarkColors.track,
        onSurfaceVariant = DarkColors.onSurfaceVariant,
        surfaceTint = DarkColors.surface,
        inverseSurface = DarkColors.onSurface,
        inverseOnSurface = DarkColors.surface,
        inversePrimary = LightColors.accent,
        error = DarkColors.danger,
        onError = DarkOnAccent,
        errorContainer = DarkColors.danger.copy(alpha = DARK_TINT_ALPHA),
        onErrorContainer = DarkColors.onSurface,
        outline = DarkColors.onSurfaceVariant,
        outlineVariant = DarkColors.sep,
        surfaceBright = DarkColors.surface,
        surfaceDim = DarkColors.background,
        surfaceContainerLowest = DarkColors.surface,
        surfaceContainerLow = DarkColors.surface,
        surfaceContainer = DarkColors.surface,
        surfaceContainerHigh = DarkColors.surface,
        surfaceContainerHighest = DarkColors.surface,
    )
}

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkScheme() else lightScheme()
    val extraColors =
        if (darkTheme) {
            AppExtraColors(
                fg3 = DarkColors.fg3,
                track = DarkColors.track,
                barBg = DarkColors.barBg,
                sheet = DarkColors.sheet,
                keypad = DarkColors.keypad,
                key = DarkColors.key,
                income = DarkColors.income,
                categoryTintAlpha = DARK_TINT_ALPHA,
            )
        } else {
            AppExtraColors(
                fg3 = LightColors.fg3,
                track = LightColors.track,
                barBg = LightColors.barBg,
                sheet = LightColors.sheet,
                keypad = LightColors.keypad,
                key = LightColors.key,
                income = LightColors.income,
                categoryTintAlpha = LIGHT_TINT_ALPHA,
            )
        }

    CompositionLocalProvider(
        LocalAppExtraColors provides extraColors,
        LocalAppTypography provides AppTypography(),
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
