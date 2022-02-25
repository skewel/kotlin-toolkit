package org.readium.r2.navigator3.core.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.readium.r2.navigator3.core.lazy.LazyListItemInfo
import org.readium.r2.navigator3.core.lazy.LazyListState
import kotlin.math.abs

internal class LazyPagerState(
    currentPage: Int = 0,
) {
    val lazyListState: LazyListState =
        LazyListState(currentPage, 0)

    val visibleItemInfo: List<LazyListItemInfo>
        get() = lazyListState.layoutInfo.visibleItemsInfo.filter { abs(it.offset) != it.size }
}

@Composable
internal fun rememberLazyPagerState(
    initialPage: Int = 0,
): LazyPagerState = remember {
    LazyPagerState(
        initialPage
    )
}