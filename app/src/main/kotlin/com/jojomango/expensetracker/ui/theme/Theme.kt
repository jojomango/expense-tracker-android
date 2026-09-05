package com.jojomango.expensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
        )
    }

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) {
            darkColorScheme(
                background = DarkColors.background,
                surface = DarkColors.surface,
                onBackground = DarkColors.onSurface,
                onSurface = DarkColors.onSurface,
                onSurfaceVariant = DarkColors.onSurfaceVariant,
                outlineVariant = DarkColors.sep,
                primary = DarkColors.accent,
                onPrimary = Color.White,
                error = DarkColors.danger,
            )
        } else {
            lightColorScheme(
                background = LightColors.background,
                surface = LightColors.surface,
                onBackground = LightColors.onSurface,
                onSurface = LightColors.onSurface,
                onSurfaceVariant = LightColors.onSurfaceVariant,
                outlineVariant = LightColors.sep,
                primary = LightColors.accent,
                onPrimary = Color.White,
                error = LightColors.danger,
            )
        }
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
            )
        }

    CompositionLocalProvider(
        LocalAppExtraColors provides extraColors,
        LocalAppTypography provides AppTypography(),
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
