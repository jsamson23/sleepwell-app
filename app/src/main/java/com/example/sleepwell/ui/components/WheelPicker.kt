package com.example.sleepwell.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * iOS-style infinite wheel picker component for Jetpack Compose
 *
 * @param items List of items to display in the wheel
 * @param initialIndex Initial selected index
 * @param onItemSelected Callback when an item is selected (provides index)
 * @param modifier Modifier for the container
 * @param itemHeight Height of each item in the wheel
 * @param visibleItemsCount Number of visible items (should be odd for centered selection)
 * @param textStyle Custom text styling function
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int = 0,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Int = 50,
    visibleItemsCount: Int = 5,
    textStyle: @Composable (item: T, isSelected: Boolean) -> Unit = { item, isSelected ->
        Text(
            text = item.toString(),
            fontSize = if (isSelected) 24.sp else 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
) {
    require(items.isNotEmpty()) { "Items list cannot be empty" }
    require(visibleItemsCount % 2 != 0) { "visibleItemsCount should be odd for centered selection" }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val coroutineScope = rememberCoroutineScope()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Calculate the middle position index
    val middleIndex = visibleItemsCount / 2

    // Track the currently selected index based on scroll position
    // We need to account for the padding spacers at the start
    val centeredItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset +
                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2

            // Find the item whose center is closest to viewport center
            val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                kotlin.math.abs(itemCenter - viewportCenter)
            }

            // Convert global index to item index (subtract the spacer offset)
            centeredItem?.let {
                (it.index - middleIndex).coerceIn(0, items.size - 1)
            } ?: 0
        }
    }

    // Notify when selection changes
    LaunchedEffect(centeredItemIndex) {
        onItemSelected(centeredItemIndex)
    }

    // Scroll to initial position on first composition
    LaunchedEffect(Unit) {
        listState.scrollToItem(initialIndex)
    }

    Box(
        modifier = modifier
            .height((itemHeight * visibleItemsCount).dp)
    ) {
        // LazyColumn for scrollable items
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height((itemHeight * visibleItemsCount).dp)
        ) {
            // Add padding items at the start to center the first item
            items(middleIndex) {
                Spacer(modifier = Modifier.height(itemHeight.dp))
            }

            // Main items
            items(items.size) { index ->
                val item = items[index]

                // Check if this item is the centered one
                val isSelected = index == centeredItemIndex

                Box(
                    modifier = Modifier
                        .height(itemHeight.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    textStyle(item, isSelected)
                }
            }

            // Add padding items at the end to center the last item
            items(middleIndex) {
                Spacer(modifier = Modifier.height(itemHeight.dp))
            }
        }

        // Gradient overlays for fade effect at top and bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((itemHeight * middleIndex).dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((itemHeight * middleIndex).dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .align(Alignment.BottomCenter)
        )

        // Selection indicator - horizontal lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight.dp)
                .align(Alignment.Center)
        ) {
            // Top line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .align(Alignment.TopCenter)
            )

            // Bottom line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Infinite wheel picker that loops items for time selection
 * Provides an illusion of infinite scrolling by repeating items
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> InfiniteWheelPicker(
    items: List<T>,
    initialIndex: Int = 0,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Int = 50,
    visibleItemsCount: Int = 5,
    textStyle: @Composable (item: T, isSelected: Boolean) -> Unit = { item, isSelected ->
        Text(
            text = item.toString(),
            fontSize = if (isSelected) 24.sp else 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
) {
    require(items.isNotEmpty()) { "Items list cannot be empty" }
    require(visibleItemsCount % 2 != 0) { "visibleItemsCount should be odd" }

    // Create a large repeated list for infinite scroll illusion
    val repeatCount = 1000
    val middleStart = (repeatCount / 2) * items.size
    val infiniteItems = List(repeatCount * items.size) { index ->
        items[index % items.size]
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = middleStart + initialIndex
    )
    val coroutineScope = rememberCoroutineScope()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val middleIndex = visibleItemsCount / 2

    // Track the currently selected index based on scroll position
    val centeredItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset +
                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2

            // Find the item whose center is closest to viewport center
            val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                kotlin.math.abs(itemCenter - viewportCenter)
            }

            // Convert global index to actual item index
            centeredItem?.let {
                (it.index - middleIndex) % items.size
            } ?: 0
        }
    }

    // Notify when selection changes
    LaunchedEffect(centeredItemIndex) {
        onItemSelected(centeredItemIndex)
    }

    // Scroll to initial position
    LaunchedEffect(Unit) {
        listState.scrollToItem(middleStart + initialIndex)
    }

    Box(
        modifier = modifier
            .height((itemHeight * visibleItemsCount).dp)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height((itemHeight * visibleItemsCount).dp)
        ) {
            // Padding at start
            items(middleIndex) {
                Spacer(modifier = Modifier.height(itemHeight.dp))
            }

            // Infinite items
            items(infiniteItems.size) { index ->
                val item = infiniteItems[index]
                val actualIndex = index % items.size
                val isSelected = actualIndex == centeredItemIndex

                Box(
                    modifier = Modifier
                        .height(itemHeight.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    textStyle(item, isSelected)
                }
            }

            // Padding at end
            items(middleIndex) {
                Spacer(modifier = Modifier.height(itemHeight.dp))
            }
        }

        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((itemHeight * middleIndex).dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((itemHeight * middleIndex).dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .align(Alignment.BottomCenter)
        )

        // Selection indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight.dp)
                .align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .align(Alignment.BottomCenter)
            )
        }
    }
}
