package com.csiss.inventory.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val RefreshContainerLight = Color(0xFFE9DDF7)
private val RefreshIconDark = Color(0xFF132945)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsissLightRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    PullToRefreshDefaults.Indicator(
        state = state,
        isRefreshing = isRefreshing,
        modifier = modifier,
        containerColor = RefreshContainerLight,
        color = RefreshIconDark
    )
}
