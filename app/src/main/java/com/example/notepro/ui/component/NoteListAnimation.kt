package com.example.notepro.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Applies a staggered animation to list items in a LazyList
 */
fun LazyListScope.animatedItems(
    count: Int,
    itemContent: @Composable LazyItemScope.(index: Int) -> Unit
) {
    items(count) { index ->
        AnimatedListItem(
            index = index,
            content = { itemContent(index) }
        )
    }
}

/**
 * Applies a staggered animation to list items in a LazyList with a collection
 */
fun <T> LazyListScope.animatedItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(item: T, index: Int) -> Unit
) {
    items(
        count = items.size,
        key = if (key != null) { index -> key(items[index]) } else null
    ) { index ->
        AnimatedListItem(
            index = index,
            content = { itemContent(items[index], index) }
        )
    }
}

/**
 * Animates a single list item with fade-in and slide-up effect
 */
@Composable
private fun AnimatedListItem(
    index: Int,
    content: @Composable () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(false).apply {
            // Start the animation immediately
            targetState = true
        }
    }
    
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300, delayMillis = index * 50)
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetY = { it * 2 } // initial offset of 2x item height
        )
    ) {
        content()
    }
}

/**
 * Apply fade-in animation to a composable
 */
@Composable
fun FadeInAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = modifier
    ) {
        content()
    }
} 