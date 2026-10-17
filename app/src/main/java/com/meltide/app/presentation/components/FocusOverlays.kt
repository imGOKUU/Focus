package com.meltide.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.meltide.app.presentation.theme.ButtonLabel
import com.meltide.app.presentation.theme.CardCornerRadius
import com.meltide.app.presentation.theme.FocusDanger

/**
 * The quieter companion to [FocusPrimaryButton]: a raised card-colored
 * button for the secondary choice (Keep going, Edit, Cancel).
 */
@Composable
fun FocusSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 52.dp
) {
    FocusSurface(
        modifier = modifier.height(height),
        depth = Depth.Low,
        onClick = onClick,
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = ButtonLabel, color = contentColor)
    }
}

/**
 * Confirmation dialog in the app's card style. [destructive] turns the
 * confirm button brick red, for choices that undo something (End, Delete).
 */
@Composable
fun FocusDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        FocusSurface(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            depth = Depth.High
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusSecondaryButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    FocusPrimaryButton(
                        text = confirmText,
                        onClick = onConfirm,
                        containerColor = if (destructive) FocusDanger else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Bottom sheet on the cream page color, with the app's card radius on top. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = CardCornerRadius, topEnd = CardCornerRadius),
        // The default handle is a pale grey that nearly vanishes on cream.
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            content = content
        )
    }
}
