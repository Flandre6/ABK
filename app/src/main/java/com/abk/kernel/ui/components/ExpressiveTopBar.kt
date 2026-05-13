@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.abk.kernel.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abk.kernel.ui.theme.uiSurfaceColor

val AbkScreenHorizontalPadding: Dp = 24.dp
private val ExpressiveTopBarCollapsedHeight: Dp = 56.dp
private val ExpressiveTopBarCompactExpandedHeight: Dp = ExpressiveTopBarCollapsedHeight
private val ExpressiveTopBarExpandedHeight: Dp = ExpressiveTopBarCollapsedHeight

@Composable
fun ExpressiveFlexibleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    compactTitle: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    ExpressiveTopBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        compactTitle = compactTitle,
        scrollBehavior = scrollBehavior,
        actions = actions
    )
}

@Composable
fun ExpressiveTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    largeTitle: Boolean = false,
    compactTitle: Boolean = false,
    collapsing: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val hasNavigation = navigationIcon != null
    val useLargeTitle = largeTitle || !hasNavigation
    val behavior = scrollBehavior
    val density = LocalDensity.current
    val expandedHeight = if (compactTitle) {
        ExpressiveTopBarCompactExpandedHeight
    } else {
        ExpressiveTopBarExpandedHeight
    }
    val rawCollapseRange = expandedHeight - ExpressiveTopBarCollapsedHeight
    val canCollapse = collapsing && useLargeTitle && behavior != null && rawCollapseRange.value > 0f
    val collapseRange = if (canCollapse) rawCollapseRange else 0.dp

    if (useLargeTitle && behavior != null) {
        SideEffect {
            behavior.state.heightOffsetLimit = -with(density) { collapseRange.toPx() }
            if (!canCollapse) {
                behavior.state.heightOffset = 0f
            }
        }
    }

    val collapsedFraction = if (canCollapse && behavior != null) {
        val limit = behavior.state.heightOffsetLimit
        if (limit != 0f) {
            (behavior.state.heightOffset / limit).coerceIn(0f, 1f)
        } else {
            0f
        }
    } else {
        0f
    }
    val expandedFraction = 1f - collapsedFraction
    val barHeight = if (useLargeTitle) {
        ExpressiveTopBarCollapsedHeight + (expandedHeight - ExpressiveTopBarCollapsedHeight) * expandedFraction
    } else {
        ExpressiveTopBarCollapsedHeight
    }

    val largeTitleStyle = if (compactTitle) {
        MaterialTheme.typography.headlineLarge.copy(
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        )
    } else {
        MaterialTheme.typography.headlineLarge.copy(
            fontSize = 44.sp,
            lineHeight = 50.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        )
    }
    val collapsedTitleStyle =
        MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = uiSurfaceColor(MaterialTheme.colorScheme.surface),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(barHeight)
                .padding(
                    start = if (hasNavigation) 4.dp else AbkScreenHorizontalPadding,
                    end = AbkScreenHorizontalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    navigationIcon()
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (useLargeTitle) {
                    Text(
                        text = title,
                        modifier = Modifier.graphicsLayer { alpha = expandedFraction },
                        style = largeTitleStyle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (useLargeTitle) collapsedFraction else 1f
                    },
                    style = collapsedTitleStyle,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
