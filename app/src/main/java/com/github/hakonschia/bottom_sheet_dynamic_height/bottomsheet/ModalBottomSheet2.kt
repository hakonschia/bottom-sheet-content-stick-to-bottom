package com.github.hakonschia.bottom_sheet_dynamic_height.bottomsheet

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.material.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.ModalBottomSheetValue.Expanded
import androidx.compose.material.ModalBottomSheetValue.HalfExpanded
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * State of the [ModalBottomSheetLayout2] composable.
 *
 * @param initialValue The initial value of the state. <b>Must not be set to
 * [ModalBottomSheetValue.HalfExpanded] if [isSkipHalfExpanded] is set to true.</b>
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param isSkipHalfExpanded Whether the half expanded state, if the sheet is tall enough, should
 * be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * <b>Must not be set to true if the [initialValue] is [ModalBottomSheetValue.HalfExpanded].</b>
 * If supplied with [ModalBottomSheetValue.HalfExpanded] for the [initialValue], an
 * [IllegalArgumentException] will be thrown.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@ExperimentalMaterialApi
class ModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    internal val isSkipHalfExpanded: Boolean,
    confirmStateChange: (ModalBottomSheetValue) -> Boolean = { true }
) : SwipeableState<ModalBottomSheetValue>(
    initialValue = initialValue,
    animationSpec = animationSpec,
    confirmStateChange = confirmStateChange
) {
    /**
     * Whether the bottom sheet is visible.
     */
    val isVisible: Boolean
        get() = currentValue != Hidden

    internal val hasHalfExpandedState: Boolean
        get() = anchors.values.contains(HalfExpanded)

    constructor(
        initialValue: ModalBottomSheetValue,
        animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
        confirmStateChange: (ModalBottomSheetValue) -> Boolean = { true }
    ) : this(initialValue, animationSpec, isSkipHalfExpanded = false, confirmStateChange)

    init {
        if (isSkipHalfExpanded) {
            require(initialValue != HalfExpanded) {
                "The initial value must not be set to HalfExpanded if skipHalfExpanded is set to" +
                        " true."
            }
        }
    }

    /**
     * Show the bottom sheet with animation and suspend until it's shown. If the sheet is taller
     * than 50% of the parent's height, the bottom sheet will be half expanded. Otherwise it will be
     * fully expanded.
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun show() {
        val targetValue = when {
            hasHalfExpandedState -> HalfExpanded
            else -> Expanded
        }
        animateTo(targetValue = targetValue)
    }

    /**
     * Half expand the bottom sheet if half expand is enabled with animation and suspend until it
     * animation is complete or cancelled
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    internal suspend fun halfExpand() {
        if (!hasHalfExpandedState) {
            return
        }
        animateTo(HalfExpanded)
    }

    /**
     * Fully expand the bottom sheet with animation and suspend until it if fully expanded or
     * animation has been cancelled.
     * *
     * @throws [CancellationException] if the animation is interrupted
     */
    internal suspend fun expand() = animateTo(Expanded)

    /**
     * Hide the bottom sheet with animation and suspend until it if fully hidden or animation has
     * been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun hide() = animateTo(Hidden)

    internal val nestedScrollConnection = this.PreUpPostDownNestedScrollConnection

    companion object {
        /**
         * The default [Saver] implementation for [ModalBottomSheetState].
         */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            skipHalfExpanded: Boolean,
            confirmStateChange: (ModalBottomSheetValue) -> Boolean
        ): Saver<ModalBottomSheetState, *> = Saver(
            save = { it.currentValue },
            restore = {
                ModalBottomSheetState(
                    initialValue = it,
                    animationSpec = animationSpec,
                    isSkipHalfExpanded = skipHalfExpanded,
                    confirmStateChange = confirmStateChange
                )
            }
        )

        /**
         * The default [Saver] implementation for [ModalBottomSheetState].
         */
        @Deprecated(
            message = "Please specify the skipHalfExpanded parameter",
            replaceWith = ReplaceWith(
                "ModalBottomSheetState.Saver(" +
                        "animationSpec = animationSpec," +
                        "skipHalfExpanded = ," +
                        "confirmStateChange = confirmStateChange" +
                        ")"
            )
        )
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (ModalBottomSheetValue) -> Boolean
        ): Saver<ModalBottomSheetState, *> = Saver(
            animationSpec = animationSpec,
            skipHalfExpanded = false,
            confirmStateChange = confirmStateChange
        )
    }
}

/**
 * Create a [ModalBottomSheetState] and [remember] it.
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param skipHalfExpanded Whether the half expanded state, if the sheet is tall enough, should
 * be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * <b>Must not be set to true if the [initialValue] is [ModalBottomSheetValue.HalfExpanded].</b>
 * If supplied with [ModalBottomSheetValue.HalfExpanded] for the [initialValue], an
 * [IllegalArgumentException] will be thrown.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterialApi
fun rememberModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    skipHalfExpanded: Boolean,
    confirmStateChange: (ModalBottomSheetValue) -> Boolean = { true }
): ModalBottomSheetState {
    return rememberSaveable(
        initialValue, animationSpec, skipHalfExpanded, confirmStateChange,
        saver = ModalBottomSheetState.Saver(
            animationSpec = animationSpec,
            skipHalfExpanded = skipHalfExpanded,
            confirmStateChange = confirmStateChange
        )
    ) {
        ModalBottomSheetState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            isSkipHalfExpanded = skipHalfExpanded,
            confirmStateChange = confirmStateChange
        )
    }
}

/**
 * Create a [ModalBottomSheetState] and [remember] it.
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterialApi
fun rememberModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (ModalBottomSheetValue) -> Boolean = { true }
): ModalBottomSheetState = rememberModalBottomSheetState(
    initialValue = initialValue,
    animationSpec = animationSpec,
    skipHalfExpanded = false,
    confirmStateChange = confirmStateChange
)

/**
 * <a href="https://material.io/components/sheets-bottom#modal-bottom-sheet" class="external" target="_blank">Material Design modal bottom sheet</a>.
 *
 * Modal bottom sheets present a set of choices while blocking interaction with the rest of the
 * screen. They are an alternative to inline menus and simple dialogs, providing
 * additional room for content, iconography, and actions.
 *
 * ![Modal bottom sheet image](https://developer.android.com/images/reference/androidx/compose/material/modal-bottom-sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material.samples.ModalBottomSheetSample
 *
 * @param sheetContent The content of the bottom sheet.
 * @param modifier Optional [Modifier] for the entire component.
 * @param sheetState The state of the bottom sheet.
 * @param sheetShape The shape of the bottom sheet.
 * @param sheetElevation The elevation of the bottom sheet.
 * @param sheetBackgroundColor The background color of the bottom sheet.
 * @param sheetContentColor The preferred content color provided by the bottom sheet to its
 * children. Defaults to the matching content color for [sheetBackgroundColor], or if that is not
 * a color from the theme, this will keep the same content color set above the bottom sheet.
 * @param scrimColor The color of the scrim that is applied to the rest of the screen when the
 * bottom sheet is visible. If the color passed is [Color.Unspecified], then a scrim will no
 * longer be applied and the bottom sheet will not block interaction with the rest of the screen
 * when visible.
 * @param content The content of rest of the screen.
 */
@Composable
@ExperimentalMaterialApi
fun ModalBottomSheetLayout2(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState =
        rememberModalBottomSheetState(Hidden),
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier) {
        val fullHeight = constraints.maxHeight.toFloat()
        val sheetHeightState = remember { mutableStateOf<Float?>(null) }
        var sheetContentHeightState by remember { mutableStateOf<Int?>(null) }
        var isSheetContentAnimating by remember { mutableStateOf(false) }

        Box(Modifier.fillMaxSize()) {
            content()
            Scrim(
                color = scrimColor,
                onDismiss = {
                    if (sheetState.confirmStateChange(Hidden)) {
                        scope.launch { sheetState.hide() }
                    }
                },
                visible = sheetState.targetValue != Hidden
            )
        }
        Surface(
            Modifier
                .fillMaxWidth()
                .nestedScroll(sheetState.nestedScrollConnection)
                .offset {
                    val y = if (sheetState.anchors.isEmpty()) {
                        // if we don't know our anchors yet, render the sheet as hidden
                        fullHeight.roundToInt()
                    } else {
                        // See comment below about the issue with using "isSheetContentAnimating" and how it is set
                        // Maybe we could instead invert this and use "isSwiping"?
                        if (isSheetContentAnimating) {
                            fullHeight.toInt() - (sheetContentHeightState ?: 0)
                        } else {
                            // if we do know our anchors, respect them
                            sheetState.offset.value.roundToInt()
                        }
                    }

                    IntOffset(0, y)
                }
                .bottomSheetSwipeable(sheetState, fullHeight, sheetHeightState)
                .onGloballyPositioned {
                    sheetHeightState.value = it.size.height.toFloat()
                }
                .semantics {
                    if (sheetState.isVisible) {
                        dismiss {
                            if (sheetState.confirmStateChange(Hidden)) {
                                scope.launch { sheetState.hide() }
                            }
                            true
                        }
                        if (sheetState.currentValue == HalfExpanded) {
                            expand {
                                if (sheetState.confirmStateChange(Expanded)) {
                                    scope.launch { sheetState.expand() }
                                }
                                true
                            }
                        } else if (sheetState.hasHalfExpandedState) {
                            collapse {
                                if (sheetState.confirmStateChange(HalfExpanded)) {
                                    scope.launch { sheetState.halfExpand() }
                                }
                                true
                            }
                        }
                    }
                },
            shape = sheetShape,
            elevation = sheetElevation,
            color = sheetBackgroundColor,
            contentColor = sheetContentColor
        ) {
            val customAnimateModifier = if (true) {
                Modifier
                    .onSizeChanged {
                        sheetContentHeightState = it.height
                    }
                    .animateContentSize(
                        startListener = {
                            isSheetContentAnimating = true
                        },
                        finishedListener = { _, _ ->
                            isSheetContentAnimating = false
                        }
                    )
            } else {
                Modifier
                    .onSizeChanged {
                        // I don't think setting "isSheetContentAnimating = true" makes sense to do here. It would be better to only set this when
                        // we know we're actually animating, ideally if "animateContentSize" had a "startListener" in addition to "finishedListener"
                        // If we set this the first time the size changes it will not be set back to false since "animateContentSize.finishedListener"
                        // won't be called, which breaks the "offset" modifier above making the bottom sheet content not swipeable until the content
                        // size animated and triggering "animateContentSize"
                        if (sheetContentHeightState != null) {
                            isSheetContentAnimating = true
                        }
                        sheetContentHeightState = it.height
                    }
                    .animateContentSize { _, _ ->
                        isSheetContentAnimating = false
                    }
            }

            Column(
                content = sheetContent,
                modifier = customAnimateModifier
            )
        }
    }
}

fun Modifier.animateContentSize(
    animationSpec: FiniteAnimationSpec<IntSize> = spring(),
    startListener: () -> Unit,
    finishedListener: ((initialValue: IntSize, targetValue: IntSize) -> Unit)? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animateContentSize"
        properties["animationSpec"] = animationSpec
        properties["finishedListener"] = finishedListener
    }
) {
    // TODO: Listener could be a fun interface after 1.4
    val scope = rememberCoroutineScope()
    val animModifier = remember(scope) {
        SizeAnimationModifier(animationSpec, scope)
    }
    animModifier.listener = finishedListener
    animModifier.startListener = startListener
    this
        .clipToBounds()
        .then(animModifier)
}

/**
 * This class creates a [LayoutModifier] that measures children, and responds to children's size
 * change by animating to that size. The size reported to parents will be the animated size.
 */
private class SizeAnimationModifier(
    val animSpec: AnimationSpec<IntSize>,
    val scope: CoroutineScope,
) : LayoutModifierWithPassThroughIntrinsics() {
    var startListener: (() -> Unit)? = null
    var listener: ((startSize: IntSize, endSize: IntSize) -> Unit)? = null

    data class AnimData(
        val anim: Animatable<IntSize, AnimationVector2D>,
        var startSize: IntSize
    )

    var animData: AnimData? by mutableStateOf(null)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val placeable = measurable.measure(constraints)

        val measuredSize = IntSize(placeable.width, placeable.height)

        val (width, height) = animateTo(measuredSize)
        return layout(width, height) {
            placeable.placeRelative(0, 0)
        }
    }

    fun animateTo(targetSize: IntSize): IntSize {
        val data = animData?.apply {
            if (targetSize != anim.targetValue) {
                startSize = anim.value
                scope.launch {
                    startListener?.invoke()
                    val result = anim.animateTo(targetSize, animSpec)
                    if (result.endReason == AnimationEndReason.Finished) {
                        listener?.invoke(startSize, result.endState.value)
                    }
                }
            }
        } ?: AnimData(
            Animatable(
                targetSize, IntSize.VectorConverter, IntSize(1, 1)
            ),
            targetSize
        )

        animData = data
        return data.anim.value
    }
}

internal abstract class LayoutModifierWithPassThroughIntrinsics : LayoutModifier {
    final override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)

    final override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)

    final override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)

    final override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)
}

@Suppress("ModifierInspectorInfo")
@OptIn(ExperimentalMaterialApi::class)
private fun Modifier.bottomSheetSwipeable(
    sheetState: ModalBottomSheetState,
    fullHeight: Float,
    sheetHeightState: State<Float?>
): Modifier {
    val sheetHeight = sheetHeightState.value
    val modifier = if (sheetHeight != null) {
        val anchors = if (sheetHeight < fullHeight / 2 || sheetState.isSkipHalfExpanded) {
            mapOf(
                fullHeight to Hidden,
                fullHeight - sheetHeight to Expanded
            )
        } else {
            mapOf(
                fullHeight to Hidden,
                fullHeight / 2 to HalfExpanded,
                max(0f, fullHeight - sheetHeight) to Expanded
            )
        }
        Modifier.swipeable(
            state = sheetState,
            anchors = anchors,
            orientation = Orientation.Vertical,
            enabled = sheetState.currentValue != Hidden,
            resistance = null
        )
    } else {
        Modifier
    }

    return this.then(modifier)
}

@Composable
private fun Scrim(
    color: Color,
    onDismiss: () -> Unit,
    visible: Boolean
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec()
        )
        val closeSheet = "Something"
        val dismissModifier = if (visible) {
            Modifier
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                .semantics(mergeDescendants = true) {
                    contentDescription = closeSheet
                    onClick { onDismiss(); true }
                }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissModifier)
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}

/**
 * Contains useful Defaults for [ModalBottomSheetLayout2].
 */
object ModalBottomSheetDefaults {

    /**
     * The default elevation used by [ModalBottomSheetLayout2].
     */
    val Elevation = 16.dp

    /**
     * The default scrim color used by [ModalBottomSheetLayout2].
     */
    val scrimColor: Color
        @Composable
        get() = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
}
