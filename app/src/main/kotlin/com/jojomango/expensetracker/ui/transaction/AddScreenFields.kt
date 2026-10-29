package com.jojomango.expensetracker.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.displayName
import com.jojomango.expensetracker.ui.i18n.rememberCategoryNames
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import kotlinx.datetime.minus

/** 網格與備註時的單列共用同一種格子（UX-001、UX-005）。 */
@Composable
internal fun CategoryCell(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = categoryColor(category)
    Column(
        // selectable：TalkBack 唸得出「已選取」，選中不能只靠外框顏色（UX-001）
        modifier = modifier.selectable(selected = selected, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(50.dp)
                    .background(color.copy(alpha = LocalAppExtraColors.current.categoryTintAlpha), RoundedCornerShape(16.dp))
                    .then(
                        if (selected) {
                            Modifier.border(2.5.dp, color, RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(category.icon, fontSize = 22.sp)
        }
        // 外框是分類色本身，色盲使用者分不出來；選中時名稱加粗（UI-SPEC §5、UX-001）
        Text(
            category.displayName(rememberCategoryNames()),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.W700 else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * 備註欄。用 BasicTextField＋外框裝飾而不是 OutlinedTextField：後者內建上下 16dp 內距、最小高 56dp，
 * 矮手機上壓到 48dp（UX-005）時文字下半截會被裁掉，這裡依高度一起縮內距。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    height: Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    val shape = RoundedCornerShape(12.dp)
    val verticalPadding = (height - 24.dp) / 2
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        // 「完成」只收鍵盤、不送出這筆（UX-002 人類決定）；備註為空也不自動收合
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(height),
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = { Text(stringResource(R.string.transaction_note)) },
            colors = colors,
            contentPadding = OutlinedTextFieldDefaults.contentPadding(top = verticalPadding, bottom = verticalPadding),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = shape,
                )
            },
        )
    }
}
