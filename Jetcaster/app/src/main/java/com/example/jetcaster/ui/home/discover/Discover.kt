/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetcaster.ui.home.discover

import androidx.animation.FloatPropKey
import androidx.animation.LinearEasing
import androidx.animation.LinearOutSlowInEasing
import androidx.animation.TransitionDefinition
import androidx.animation.transitionDefinition
import androidx.animation.tween
import androidx.compose.Composable
import androidx.compose.collectAsState
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.drawLayer
import androidx.ui.foundation.Border
import androidx.ui.foundation.Text
import androidx.ui.foundation.contentColor
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.material.EmphasisAmbient
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.material.Tab
import androidx.ui.material.TabRow
import androidx.ui.unit.dp
import androidx.ui.viewmodel.viewModel
import com.example.jetcaster.data.Category
import com.example.jetcaster.ui.home.PodcastCategory
import com.example.jetcaster.util.ItemSwitcher
import com.example.jetcaster.util.ItemTransitionState

@Composable
fun Discover(
    modifier: Modifier = Modifier
) {
    val viewModel: DiscoverViewModel = viewModel()
    val viewState by viewModel.state.collectAsState()

    val selectedCategory = viewState.selectedCategory

    if (viewState.categories.isNotEmpty() && selectedCategory != null) {
        Column(modifier) {
            Spacer(Modifier.preferredHeight(8.dp))

            // We need to keep track of the previously selected category, to determine the
            // change direction below for the transition
            var previousSelectedCategory by state<Category?> { null }

            PodcastCategoryTabs(
                categories = viewState.categories,
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.preferredHeight(8.dp))

            // We need to reverse the transition if the new category is to the left/start
            // of the previous category
            val reverseTransition = previousSelectedCategory?.let { p ->
                viewState.categories.indexOf(selectedCategory) < viewState.categories.indexOf(p)
            } ?: false
            val transitionOffset = with(DensityAmbient.current) { 16.dp.toPx() }

            ItemSwitcher(
                current = selectedCategory,
                transitionDefinition = getChoiceChipTransitionDefinition(
                    reverse = reverseTransition,
                    offsetPx = transitionOffset
                ),
                modifier = Modifier.fillMaxWidth()
                    .weight(1f)
            ) { category, transitionState ->
                /**
                 * TODO, need to think about how this will scroll within the outer VerticalScroller
                 */
                PodcastCategory(
                    categoryId = category.id,
                    modifier = Modifier.fillMaxSize()
                        .drawLayer(
                            translationX = transitionState[Offset],
                            alpha = transitionState[Alpha]
                        )
                )
            }

            onCommit(selectedCategory) {
                // Update our tracking of the previously selected category
                previousSelectedCategory = selectedCategory
            }
        }
    } else {
        // TODO: empty state
    }
}

@Composable
private fun PodcastCategoryTabs(
    categories: List<Category>,
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = categories.indexOfFirst { it == selectedCategory }
    TabRow(
        items = categories,
        selectedIndex = selectedIndex,
        scrollable = true,
        divider = emptyContent(), /* Disable the built-in divider */
        indicatorContainer = { _ -> },
        modifier = modifier
    ) { index, category ->
        Tab(
            selected = index == selectedIndex,
            onSelected = { onCategorySelected(category) }
        ) {
            ChoiceChipContent(
                text = category.name,
                selected = index == selectedIndex,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun ChoiceChipContent(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            selected -> MaterialTheme.colors.primary.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        contentColor = when {
            selected -> MaterialTheme.colors.primary
            else -> EmphasisAmbient.current.high.applyEmphasis(contentColor())
        },
        shape = MaterialTheme.shapes.small,
        border = Border(
            size = 1.dp,
            color = when {
                selected -> MaterialTheme.colors.primary
                else -> EmphasisAmbient.current.disabled.applyEmphasis(contentColor())
            }
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private val Alpha = FloatPropKey()
private val Offset = FloatPropKey()

@Composable
private fun getChoiceChipTransitionDefinition(
    duration: Int = 183,
    offsetPx: Float,
    reverse: Boolean = false
): TransitionDefinition<ItemTransitionState> = remember(reverse, offsetPx, duration) {
    transitionDefinition {
        state(ItemTransitionState.Visible) {
            this[Alpha] = 1f
            this[Offset] = 0f
        }
        state(ItemTransitionState.BecomingVisible) {
            this[Alpha] = 0f
            this[Offset] = if (reverse) -offsetPx else offsetPx
        }
        state(ItemTransitionState.BecomingNotVisible) {
            this[Alpha] = 0f
            this[Offset] = if (reverse) offsetPx else -offsetPx
        }

        val halfDuration = duration / 2

        transition(
            fromState = ItemTransitionState.BecomingVisible,
            toState = ItemTransitionState.Visible
        ) {
            // TODO: look at whether this can be implemented using `spring` to enable
            //  interruptions, etc
            Alpha using tween(
                durationMillis = halfDuration,
                delayMillis = halfDuration,
                easing = LinearEasing
            )
            Offset using tween(
                durationMillis = halfDuration,
                delayMillis = halfDuration,
                easing = LinearOutSlowInEasing
            )
        }

        transition(
            fromState = ItemTransitionState.Visible,
            toState = ItemTransitionState.BecomingNotVisible
        ) {
            Alpha using tween(
                durationMillis = halfDuration,
                easing = LinearEasing,
                delayMillis = DelayForContentToLoad
            )
            Offset using tween(
                durationMillis = halfDuration,
                easing = LinearOutSlowInEasing,
                delayMillis = DelayForContentToLoad
            )
        }
    }
}

/**
 * This is a hack. Compose currently has no concept of delayed transitions, something akin to
 * Fragment postponing of transitions while content loads. To workaround that for now, we
 * introduce an initial hardcoded delay of 24ms.
 */
private const val DelayForContentToLoad = 24
