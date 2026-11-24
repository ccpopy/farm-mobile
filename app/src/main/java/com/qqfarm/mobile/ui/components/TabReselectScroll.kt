package com.qqfarm.mobile.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class TabReselectSignal(
    val tabIndex: Int = -1,
    val seq: Int = 0,
)

val LocalTabReselectSignal = compositionLocalOf { TabReselectSignal() }

@Composable
fun ScrollToTopOnTabReselect(tabIndex: Int, listState: LazyListState) {
    OnTabReselect(tabIndex, listState) {
        val layout = listState.layoutInfo
        listState.animateTabReturnToTop(
            firstIndex = listState.firstVisibleItemIndex,
            firstOffset = listState.firstVisibleItemScrollOffset,
            viewportHeight = layout.viewportSize.height,
            visibleCount = layout.visibleItemsInfo.size,
            snapToTop = { offset -> listState.scrollToItem(0, offset) },
        )
    }
}

@Composable
fun ScrollToTopOnTabReselect(tabIndex: Int, gridState: LazyGridState) {
    OnTabReselect(tabIndex, gridState) {
        val layout = gridState.layoutInfo
        gridState.animateTabReturnToTop(
            firstIndex = gridState.firstVisibleItemIndex,
            firstOffset = gridState.firstVisibleItemScrollOffset,
            viewportHeight = layout.viewportSize.height,
            visibleCount = layout.visibleItemsInfo.size,
            snapToTop = { offset -> gridState.scrollToItem(0, offset) },
        )
    }
}

@Composable
private fun OnTabReselect(tabIndex: Int, state: ScrollableState, scrollToTop: suspend () -> Unit) {
    val signal by rememberUpdatedState(LocalTabReselectSignal.current)
    LaunchedEffect(tabIndex, state) {
        var handledSeq = signal.seq
        var animation: Job? = null
        snapshotFlow { signal }.collect { event ->
            if (event.tabIndex != tabIndex) {
                animation?.cancel()
            } else if (event.seq > handledSeq) {
                handledSeq = event.seq
                if (animation?.isActive != true) animation = launch { scrollToTop() }
            }
        }
    }
}

internal suspend fun ScrollableState.animateTabReturnToTop(
    firstIndex: Int,
    firstOffset: Int,
    viewportHeight: Int,
    visibleCount: Int,
    snapToTop: suspend (Int) -> Unit,
) {
    if (!canScrollBackward || viewportHeight <= 0) return

    val screensFromTop = firstIndex.toFloat() / visibleCount.coerceAtLeast(1)
    val travelLimit = 3 * viewportHeight
    val distance = when {
        (firstIndex >= 24 && screensFromTop >= 3f) || firstOffset > travelLimit -> {
            // 深处回顶时先在顶部附近就位，之后只跑一段减速滑动。
            val landingOffset = viewportHeight / 2
            snapToTop(landingOffset)
            landingOffset
        }
        firstIndex == 0 -> firstOffset
        else -> (viewportHeight * (screensFromTop + 0.75f) + firstOffset)
            .roundToInt().coerceAtMost(travelLimit)
    }

    val duration = (160 + 50f * distance / viewportHeight).roundToInt().coerceIn(180, 280)
    animateScrollBy(-distance.toFloat(), tween(durationMillis = duration, easing = FastOutSlowInEasing))
    if (canScrollBackward) snapToTop(0)
}
