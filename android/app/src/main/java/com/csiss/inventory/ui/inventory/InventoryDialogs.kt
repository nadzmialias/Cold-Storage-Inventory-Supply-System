package com.csiss.inventory.ui.inventory

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun InventoryDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = lightColorScheme(), typography = Typography()) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) {
                    title()
                }
            },
            text = { ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() } },
            confirmButton = confirmButton,
            dismissButton = dismissButton
        )
    }
}
