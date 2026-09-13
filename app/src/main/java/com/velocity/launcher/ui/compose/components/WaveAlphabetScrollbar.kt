package com.velocity.launcher.ui.compose.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.ScrollbarPosition
import com.velocity.launcher.data.ScrollbarVerticalAlignment
import com.velocity.launcher.ui.theme.AppLabelFontSize
import kotlin.math.abs
import kotlin.math.exp
import kotlinx.coroutines.isActive

val DEFAULT_ANCHOR_LETTERS: Set<String> = setOf("★", "A", "E", "I", "M", "Q", "U", "Z", "#", "🔍", "⚙")
val ACTION_LETTERS: Set<String> = setOf("🔍", "⚙")

const val STABLE_RAIL_SCALE = 1.0f
const val DEFAULT_VERTICAL_SPREAD_FACTOR = 0.20f
val MIN_WAVE_DISPLACEMENT_DP: Dp = 64.dp
const val MAX_WAVE_DISPLACEMENT_SCREEN_FRACTION = 0.5f
val DEFAULT_WAVE_DISPLACEMENT_DP: Dp = 72.dp
val FINGER_CLEARANCE_DP: Dp = 44.dp
val FLOATING_BUBBLE_SIZE_DP: Dp = 34.dp
val FLOATING_BUBBLE_GAP_DP: Dp = 12.dp
val FLOATING_BUBBLE_ICON_SIZE_DP: Dp = 18.dp

const val DRAG_SPRING_ANGULAR_FREQUENCY = 75f
const val DISPLACEMENT_SPRING_ANGULAR_FREQUENCY = 50f
val SCROLLBAR_TOUCH_WIDTH_DP: Dp = 56.dp

/**
 * Contiguous primitive memory buffers (Data-Oriented Design)
 * to avoid allocations during 60/120 FPS draw iterations.
 */
class WaveRenderBuffer(val capacity: Int) {
    val centroids: FloatArray = FloatArray(capacity)
    val displacements: FloatArray = FloatArray(capacity)
    val scales: FloatArray = FloatArray(capacity)
    val alphas: FloatArray = FloatArray(capacity)
    val isBold: BooleanArray = BooleanArray(capacity)
    val isDot: BooleanArray = BooleanArray(capacity)
    val isActive: BooleanArray = BooleanArray(capacity)
}

class CriticalDampedSpringState(
    var position: Float = 0f,
    var velocity: Float = 0f
)

/**
 * Layout and text measurement cache (OOP) to pre-measure glyphs and anchor flags.
 */
class WaveAlphabetLayoutCache(
    val alphabet: List<String>,
    val baseFontSize: TextUnit = AppLabelFontSize,
    textMeasurer: TextMeasurer,
    anchorLetters: Set<String> = DEFAULT_ANCHOR_LETTERS
) {
    companion object {
        val NORMAL_FONT_WEIGHT = FontWeight.Medium
        val ANCHOR_FONT_WEIGHT = FontWeight.Medium
    }

    val size: Int = alphabet.size
    val isAnchor: BooleanArray = BooleanArray(size) { index ->
        val letter = alphabet[index]
        letter in anchorLetters || index == 0 || index == alphabet.lastIndex
    }

    val normalStyle: TextStyle
    val anchorStyle: TextStyle
    val boldStyle: TextStyle

    private val normalLayouts: Array<TextLayoutResult>
    private val boldLayouts: Array<TextLayoutResult>
    val dotLayout: TextLayoutResult

    init {
        val letterShadow = Shadow(
            color = Color.Black.copy(alpha = 0.65f),
            offset = Offset(0f, 2f),
            blurRadius = 4f
        )
        normalStyle = TextStyle(
            fontSize = baseFontSize,
            fontWeight = NORMAL_FONT_WEIGHT,
            textAlign = TextAlign.Center,
            shadow = letterShadow
        )
        anchorStyle = TextStyle(
            fontSize = baseFontSize,
            fontWeight = ANCHOR_FONT_WEIGHT,
            textAlign = TextAlign.Center,
            shadow = letterShadow
        )
        boldStyle = TextStyle(
            fontSize = baseFontSize,
            fontWeight = NORMAL_FONT_WEIGHT,
            textAlign = TextAlign.Center,
            shadow = letterShadow
        )

        normalLayouts = Array(size) { i ->
            val style = if (isAnchor[i]) anchorStyle else normalStyle
            textMeasurer.measure(alphabet[i], style = style)
        }

        boldLayouts = Array(size) { i ->
            textMeasurer.measure(alphabet[i], style = boldStyle)
        }

        dotLayout = textMeasurer.measure("•", style = normalStyle)
    }

    fun getLayoutResult(index: Int, isBold: Boolean): TextLayoutResult {
        return if (isBold) boldLayouts[index] else normalLayouts[index]
    }
}

/**
 * Encapsulated Multi-Sensory Haptic Feedback Manager with dedicated vibration profiles.
 */
class HapticFeedbackManager(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        @Suppress("DEPRECATION")
        vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val tickEffect: VibrationEffect? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        VibrationEffect.createOneShot(10, (255 * 0.20f).toInt().coerceIn(1, 255))
    } else null

    private val clickEffect: VibrationEffect? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        VibrationEffect.createOneShot(15, (255 * 0.40f).toInt().coerceIn(1, 255))
    } else null

    private val heavyClickEffect: VibrationEffect? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        VibrationEffect.createOneShot(25, 255)
    } else null

    private val doubleClickEffect: VibrationEffect? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        VibrationEffect.createWaveform(longArrayOf(0, 12, 35, 12), intArrayOf(0, 115, 0, 115), -1)
    } else null

    fun performHaptic(enabled: Boolean = true, effect: VibrationEffect?, fallbackDurationMs: Long = 10) {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && effect != null) {
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(fallbackDurationMs)
            }
        } catch (_: Exception) {}
    }

    fun performScrollTick(enabled: Boolean = true) = performHaptic(enabled, tickEffect, 10)
    fun performSpringPop(enabled: Boolean = true) = performHaptic(enabled, clickEffect, 15)
    fun performSnapTick(enabled: Boolean = true) = performHaptic(enabled, tickEffect, 8)
    fun performLaunchHeavyBuzz(enabled: Boolean = true) = performHaptic(enabled, heavyClickEffect, 30)

    fun performHapticFeedback(enabled: Boolean = true) = performScrollTick(enabled)
}

/**
 * Wave physics engine (OOP & Functional Programming) encapsulating pure mathematical
 * wave equations, displacement, scale, alpha, and geometry calculations.
 */
object WavePhysicsEngine {
    fun stepCriticallyDampedSpring(
        state: CriticalDampedSpringState,
        target: Float,
        dtSeconds: Float,
        angularFrequency: Float = 24f
    ) {
        if (dtSeconds <= 0f || angularFrequency <= 0f) return
        val dt = dtSeconds.coerceAtMost(0.05f)
        val displacement = state.position - target
        val correction = state.velocity + angularFrequency * displacement
        val decay = exp(-angularFrequency * dt)
        state.position = target + (displacement + correction * dt) * decay
        state.velocity = (state.velocity - angularFrequency * correction * dt) * decay
    }

    fun calculateGaussianFactor(distance: Float, sigma: Float): Float {
        if (sigma <= 0f) return 0f
        val norm = distance / sigma
        return exp(-0.5f * norm * norm)
    }

    fun calculateWaveFactor(distance: Float, sigma: Float, waveProgress: Float): Float {
        return calculateGaussianFactor(distance, sigma) * waveProgress
    }

    fun calculateDisplacement(waveFactor: Float, maxDisplacementPx: Float, isRightSide: Boolean): Float {
        val sign = if (isRightSide) -1f else 1f
        return sign * maxDisplacementPx * waveFactor
    }

    fun calculateScale(waveFactor: Float = 0f, isActive: Boolean = false): Float {
        return STABLE_RAIL_SCALE
    }

    fun calculateVerticalDisplacement(
        deltaY: Float,
        waveFactor: Float,
        maxSpreadPx: Float
    ): Float {
        if (deltaY == 0f || waveFactor <= 0f || maxSpreadPx <= 0f) return 0f
        val direction = if (deltaY > 0f) 1f else -1f
        return direction * maxSpreadPx * waveFactor
    }

    fun calculateDistanceFromEdge(
        touchX: Float,
        railWidthPx: Float,
        isRightSide: Boolean
    ): Float {
        return if (isRightSide) {
            railWidthPx - touchX
        } else {
            touchX
        }
    }

    fun calculateDynamicDisplacement(
        distanceFromEdgePx: Float,
        minDisplacementPx: Float,
        maxDisplacementPx: Float,
        fingerClearancePx: Float = 0f
    ): Float {
        return (distanceFromEdgePx + fingerClearancePx).coerceIn(minDisplacementPx, maxDisplacementPx)
    }

    fun calculateBubbleX(
        railLeftX: Float,
        defaultCenterX: Float,
        displacementPx: Float,
        bubbleSizePx: Float,
        bubbleGapPx: Float,
        isRightSide: Boolean,
        totalViewportWidthPx: Float = 0f
    ): Float {
        val crestX = railLeftX + defaultCenterX + displacementPx
        val rawBubbleX = if (isRightSide) {
            crestX - bubbleSizePx - bubbleGapPx
        } else {
            crestX + bubbleGapPx
        }
        return if (totalViewportWidthPx > 0f) {
            rawBubbleX.coerceIn(0f, (totalViewportWidthPx - bubbleSizePx).coerceAtLeast(0f))
        } else {
            rawBubbleX
        }
    }

    fun calculateAlpha(waveFactor: Float, waveProgress: Float, isActive: Boolean, isDot: Boolean): Float {
        if (isActive) return 1.0f
        val rawAlpha = if (waveProgress > 0.01f) (0.55f + 0.45f * waveFactor) else 0.90f
        return if (isDot) rawAlpha * 0.75f else rawAlpha
    }

    fun calculateIndexFromTouchY(touchY: Float, totalHeight: Float, itemCount: Int): Int {
        if (itemCount <= 0 || totalHeight <= 0f) return 0
        val itemHeight = totalHeight / itemCount
        val index = (touchY / itemHeight).toInt()
        return index.coerceIn(0, itemCount - 1)
    }

    fun calculateCentroidY(index: Int, itemHeight: Float): Float {
        return (index + 0.5f) * itemHeight
    }

    fun calculateBubbleY(touchY: Float, bubbleHeight: Float, totalHeight: Float): Float {
        return (touchY - bubbleHeight / 2f).coerceIn(0f, (totalHeight - bubbleHeight).coerceAtLeast(0f))
    }

    fun calculateTargetTouchY(
        isDragging: Boolean,
        touchY: Float,
        currentScrollIndex: Int,
        itemHeightPx: Float
    ): Float {
        return if (isDragging) {
            touchY
        } else if (currentScrollIndex >= 0) {
            (currentScrollIndex + 0.5f) * itemHeightPx
        } else {
            touchY
        }
    }

    fun calculateDisplayedActiveIndex(
        isDragging: Boolean,
        waveProgress: Float,
        activeIndex: Int,
        currentScrollIndex: Int
    ): Int {
        return if (isDragging) {
            activeIndex
        } else {
            currentScrollIndex
        }
    }

    fun resolveSectionIndex(indices: IntArray, scrollItemIndex: Int): Int {
        if (indices.isEmpty()) return -1
        val searchResult = indices.binarySearch(scrollItemIndex)
        return if (searchResult >= 0) {
            searchResult
        } else {
            val insertionPoint = -searchResult - 1
            if (insertionPoint > 0) insertionPoint - 1 else 0
        }
    }

    fun resolveLetterForScrollIndex(
        indices: IntArray,
        letters: Array<String>,
        firstVisibleItemIndex: Int,
        fallback: String? = null
    ): String? {
        if (indices.isEmpty() || letters.isEmpty()) return fallback
        val matchIndex = resolveSectionIndex(indices, firstVisibleItemIndex)
        return if (matchIndex in letters.indices) letters[matchIndex] else fallback
    }

    fun resolveFocalLetter(
        indices: IntArray,
        letters: Array<String>,
        visibleItemOffsets: List<Pair<Int, Int>>,
        focalOffsetPx: Int,
        fallback: String? = null
    ): String? {
        if (indices.isEmpty() || letters.isEmpty() || visibleItemOffsets.isEmpty()) return fallback
        val focalItem = visibleItemOffsets.firstOrNull { it.second >= focalOffsetPx - 8 }
            ?: visibleItemOffsets.lastOrNull { it.second <= focalOffsetPx }
            ?: visibleItemOffsets.first()
        val matchIndex = resolveSectionIndex(indices, focalItem.first)
        return if (matchIndex in letters.indices) letters[matchIndex] else fallback
    }

    fun resolveVisibleLetters(
        indices: IntArray,
        letters: Array<String>,
        firstVisibleItemIndex: Int,
        lastVisibleItemIndex: Int,
        fallback: Set<String> = emptySet()
    ): Set<String> {
        if (indices.isEmpty() || letters.isEmpty()) return fallback
        val startIndex = resolveSectionIndex(indices, firstVisibleItemIndex)
        val endIndex = resolveSectionIndex(indices, lastVisibleItemIndex)
        if (startIndex == -1 || endIndex == -1) return fallback

        val from = startIndex.coerceIn(0, letters.lastIndex)
        val to = endIndex.coerceIn(from, letters.lastIndex)

        val result = LinkedHashSet<String>(to - from + 1)
        for (i in from..to) {
            result.add(letters[i])
        }
        return if (result.isEmpty()) fallback else result
    }

    fun computeTransforms(
        buffer: WaveRenderBuffer,
        itemCount: Int,
        itemHeightPx: Float,
        animatedTouchY: Float,
        waveProgress: Float,
        sigmaPx: Float,
        maxDisplacementPx: Float,
        isRightSide: Boolean,
        isCompact: Boolean,
        isAnchor: BooleanArray,
        activeIndex: Int = -1,
        activeIndices: Set<Int>? = null,
        isItemActive: ((Int) -> Boolean)? = null,
        alphabet: List<String>? = null,
        currentLetters: Set<String>? = null,
        maxVerticalSpreadPx: Float = itemHeightPx * DEFAULT_VERTICAL_SPREAD_FACTOR
    ) {
        val count = itemCount.coerceAtMost(buffer.capacity)
        for (i in 0 until count) {
            val baseY = (i + 0.5f) * itemHeightPx
            val deltaY = baseY - animatedTouchY
            val dist = abs(deltaY)
            val rawWave = calculateGaussianFactor(dist, sigmaPx)
            val waveFactor = rawWave * waveProgress

            val verticalShift = calculateVerticalDisplacement(deltaY, waveFactor, maxVerticalSpreadPx)
            val centerY = baseY + verticalShift
            buffer.centroids[i] = centerY

            val isActive = when {
                activeIndex >= 0 -> (i == activeIndex)
                activeIndices != null -> activeIndices.contains(i)
                isItemActive != null -> isItemActive(i)
                alphabet != null && currentLetters != null && i < alphabet.size -> alphabet[i] in currentLetters
                else -> false
            }
            buffer.isActive[i] = isActive

            val isDot = isCompact && !isAnchor[i] && !isActive && waveFactor < 0.25f
            buffer.isDot[i] = isDot

            buffer.alphas[i] = calculateAlpha(waveFactor, waveProgress, isActive, isDot)
            buffer.scales[i] = calculateScale(waveFactor, isActive)
            buffer.displacements[i] = calculateDisplacement(waveFactor, maxDisplacementPx, isRightSide)
            buffer.isBold[i] = isActive
        }
    }
}

@Composable
fun WaveAlphabetScrollbar(
    position: ScrollbarPosition,
    alphabet: List<String>,
    currentLetters: () -> Set<String>,
    focusedLetter: () -> String? = { null },
    verticalAlignment: ScrollbarVerticalAlignment = ScrollbarVerticalAlignment.BOTTOM,
    hapticEnabled: Boolean,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveAlphabet = if (alphabet.isNotEmpty()) alphabet else listOf("★")

    val context = LocalContext.current
    val hapticManager = remember(context) { HapticFeedbackManager(context) }

    val isRight = position == ScrollbarPosition.RIGHT || position == ScrollbarPosition.BOTH
    val isLeft = position == ScrollbarPosition.LEFT || position == ScrollbarPosition.BOTH

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val maxScrollbarHeight = when {
            maxHeight >= 600.dp -> (maxHeight * 0.72f).coerceIn(360.dp, 500.dp)
            maxHeight >= 400.dp -> maxHeight * 0.88f
            else -> maxHeight * 0.96f
        }
        val targetScrollbarHeight = (22.dp * effectiveAlphabet.size).coerceIn(100.dp, maxScrollbarHeight)

        if (isLeft) {
            SingleSideWaveScrollbar(
                alphabet = effectiveAlphabet,
                currentLetters = currentLetters,
                focusedLetter = focusedLetter,
                targetScrollbarHeight = targetScrollbarHeight,
                isRightSide = false,
                verticalAlignment = verticalAlignment,
                hapticEnabled = hapticEnabled,
                hapticManager = hapticManager,
                onLetterSelected = onLetterSelected,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isRight) {
            SingleSideWaveScrollbar(
                alphabet = effectiveAlphabet,
                currentLetters = currentLetters,
                focusedLetter = focusedLetter,
                targetScrollbarHeight = targetScrollbarHeight,
                isRightSide = true,
                verticalAlignment = verticalAlignment,
                hapticEnabled = hapticEnabled,
                hapticManager = hapticManager,
                onLetterSelected = onLetterSelected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun WaveAlphabetScrollbar(
    position: ScrollbarPosition,
    alphabet: List<String>,
    currentLetters: Set<String> = emptySet(),
    focusedLetter: String? = null,
    verticalAlignment: ScrollbarVerticalAlignment = ScrollbarVerticalAlignment.BOTTOM,
    hapticEnabled: Boolean,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    WaveAlphabetScrollbar(
        position = position,
        alphabet = alphabet,
        currentLetters = { currentLetters },
        focusedLetter = { focusedLetter },
        verticalAlignment = verticalAlignment,
        hapticEnabled = hapticEnabled,
        onLetterSelected = onLetterSelected,
        modifier = modifier
    )
}

@Composable
private fun SingleSideWaveScrollbar(
    alphabet: List<String>,
    currentLetters: () -> Set<String>,
    focusedLetter: () -> String? = { null },
    targetScrollbarHeight: Dp,
    isRightSide: Boolean,
    verticalAlignment: ScrollbarVerticalAlignment,
    hapticEnabled: Boolean,
    hapticManager: HapticFeedbackManager,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    var touchY by remember { mutableFloatStateOf(0f) }
    var activeIndex by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current

    val currentOnLetterSelected by rememberUpdatedState(onLetterSelected)
    val currentHapticEnabled by rememberUpdatedState(hapticEnabled)

    val focusedIndex = remember(alphabet) {
        derivedStateOf {
            val focused = focusedLetter()
            val current = currentLetters()
            if (focused != null) {
                val idx = alphabet.indexOf(focused)
                if (idx >= 0) idx else alphabet.indexOfFirst { it in current }
            } else {
                alphabet.indexOfFirst { it in current }
            }
        }
    }

    val activeLetter = if (isDragging && activeIndex in alphabet.indices) alphabet[activeIndex] else null
    var lastActiveLetter by remember { mutableStateOf("") }
    val letterToShow = activeLetter ?: lastActiveLetter

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val totalViewportWidthPx = with(density) { maxWidth.toPx() }
        val totalViewportHeightPx = with(density) { maxHeight.toPx() }
        val rootViewportSize = remember(totalViewportWidthPx, totalViewportHeightPx) {
            Size(totalViewportWidthPx, totalViewportHeightPx)
        }

        val targetScrollbarHeightPx = with(density) { targetScrollbarHeight.toPx() }
        val railWidthDp = 44.dp
        val railWidthPxInitial = with(density) { railWidthDp.toPx() }
        val itemHeightDp = if (alphabet.isNotEmpty()) targetScrollbarHeight / alphabet.size else 0.dp
        val itemHeightPx = with(density) { itemHeightDp.toPx() }
        val baseFontSize = AppLabelFontSize
        val isCompact = itemHeightDp < 13.dp

        val railAlignment = when (verticalAlignment) {
            ScrollbarVerticalAlignment.BOTTOM -> if (isRightSide) Alignment.BottomEnd else Alignment.BottomStart
            ScrollbarVerticalAlignment.CENTER -> if (isRightSide) Alignment.CenterEnd else Alignment.CenterStart
            ScrollbarVerticalAlignment.TOP -> if (isRightSide) Alignment.TopEnd else Alignment.TopStart
        }
        val railPadding = when (verticalAlignment) {
            ScrollbarVerticalAlignment.BOTTOM -> Modifier.padding(bottom = 24.dp)
            ScrollbarVerticalAlignment.TOP -> Modifier.padding(top = 16.dp)
            ScrollbarVerticalAlignment.CENTER -> Modifier
        }

        val initialRailTopY = when (verticalAlignment) {
            ScrollbarVerticalAlignment.BOTTOM -> totalViewportHeightPx - targetScrollbarHeightPx - with(density) { 24.dp.toPx() }
            ScrollbarVerticalAlignment.TOP -> with(density) { 16.dp.toPx() }
            ScrollbarVerticalAlignment.CENTER -> (totalViewportHeightPx - targetScrollbarHeightPx) / 2f
        }
        val initialRailLeftX = if (isRightSide) totalViewportWidthPx - railWidthPxInitial else 0f

        var railTopY by remember { mutableFloatStateOf(initialRailTopY) }
        var railLeftX by remember { mutableFloatStateOf(initialRailLeftX) }
        var railHeightPx by remember { mutableFloatStateOf(targetScrollbarHeightPx) }
        var railWidthPx by remember { mutableFloatStateOf(railWidthPxInitial) }

        val sigmaPx = with(density) { 130.dp.toPx() }
        val minDisplacementPx = with(density) { MIN_WAVE_DISPLACEMENT_DP.toPx() }
        val maxAllowedDisplacementPx = totalViewportWidthPx * MAX_WAVE_DISPLACEMENT_SCREEN_FRACTION
        val defaultDisplacementPx = with(density) { DEFAULT_WAVE_DISPLACEMENT_DP.toPx() }
            .coerceIn(minDisplacementPx, maxAllowedDisplacementPx)
        val fingerClearancePx = with(density) { FINGER_CLEARANCE_DP.toPx() }

        var targetDisplacementPx by remember { mutableFloatStateOf(defaultDisplacementPx) }
        val animatedDisplacementPx = remember { Animatable(defaultDisplacementPx) }
        val animatedTouchY = remember { Animatable(0f) }

        LaunchedEffect(isDragging) {
            if (!isDragging) {
                animatedDisplacementPx.animateTo(
                    targetValue = defaultDisplacementPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                return@LaunchedEffect
            }

            animatedTouchY.snapTo(touchY)
            animatedDisplacementPx.snapTo(targetDisplacementPx)
            val springStateY = CriticalDampedSpringState(
                position = touchY,
                velocity = 0f
            )
            val springStateDisp = CriticalDampedSpringState(
                position = targetDisplacementPx,
                velocity = 0f
            )
            var previousFrameNanos = 0L
            while (isActive) {
                val frameNanos = withFrameNanos { it }
                if (previousFrameNanos != 0L) {
                    val dtSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                    WavePhysicsEngine.stepCriticallyDampedSpring(
                        state = springStateY,
                        target = touchY,
                        dtSeconds = dtSeconds,
                        angularFrequency = DRAG_SPRING_ANGULAR_FREQUENCY
                    )
                    WavePhysicsEngine.stepCriticallyDampedSpring(
                        state = springStateDisp,
                        target = targetDisplacementPx,
                        dtSeconds = dtSeconds,
                        angularFrequency = DISPLACEMENT_SPRING_ANGULAR_FREQUENCY
                    )
                    animatedTouchY.snapTo(springStateY.position)
                    animatedDisplacementPx.snapTo(springStateDisp.position)
                }
                previousFrameNanos = frameNanos
            }
        }

        LaunchedEffect(focusedIndex.value, isDragging, itemHeightPx) {
            val targetIdx = focusedIndex.value
            if (!isDragging && targetIdx >= 0 && itemHeightPx > 0f) {
                animatedTouchY.snapTo((targetIdx + 0.5f) * itemHeightPx)
            }
        }

        val waveProgress by animateFloatAsState(
            targetValue = if (isDragging) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "waveProgress"
        )

        val textMeasurer = rememberTextMeasurer()
        val layoutCache = remember(alphabet, baseFontSize, textMeasurer) {
            WaveAlphabetLayoutCache(
                alphabet = alphabet,
                baseFontSize = baseFontSize,
                textMeasurer = textMeasurer,
                anchorLetters = DEFAULT_ANCHOR_LETTERS
            )
        }

        val renderBuffer = remember(alphabet.size) {
            WaveRenderBuffer(alphabet.size)
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        val defaultTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f)
        val searchPainter = rememberVectorPainter(Icons.Default.Search)

        val bubbleSizeDp = FLOATING_BUBBLE_SIZE_DP
        val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }
        val bubbleGapPx = with(density) { FLOATING_BUBBLE_GAP_DP.toPx() }
        val bubbleIconSizeDp = FLOATING_BUBBLE_ICON_SIZE_DP

        val bubbleAlpha by animateFloatAsState(
            targetValue = if (isDragging && activeLetter != null) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
            label = "bubbleAlpha"
        )

        val bubbleScale by animateFloatAsState(
            targetValue = if (isDragging && activeLetter != null) 1f else 0.7f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
            label = "bubbleScale"
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Single Hardware-Accelerated Canvas for Alphabet Rail
            Box(
                modifier = Modifier
                    .align(railAlignment)
                    .then(railPadding)
                    .width(railWidthDp)
                    .height(targetScrollbarHeight)
                    .onGloballyPositioned { coordinates ->
                        val pos = coordinates.positionInParent()
                        railTopY = pos.y
                        railLeftX = pos.x
                        railHeightPx = coordinates.size.height.toFloat()
                        railWidthPx = coordinates.size.width.toFloat()
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val totalHeight = size.height
                    val currentItemHeightPx = if (alphabet.isNotEmpty() && totalHeight > 0f) {
                        totalHeight / alphabet.size
                    } else 0f
                    if (currentItemHeightPx <= 0f || alphabet.isEmpty()) return@Canvas

                    WavePhysicsEngine.computeTransforms(
                        buffer = renderBuffer,
                        itemCount = alphabet.size,
                        itemHeightPx = currentItemHeightPx,
                        animatedTouchY = animatedTouchY.value,
                        waveProgress = waveProgress,
                        sigmaPx = sigmaPx,
                        maxDisplacementPx = animatedDisplacementPx.value,
                        isRightSide = isRightSide,
                        isCompact = isCompact,
                        isAnchor = layoutCache.isAnchor,
                        activeIndex = if (isDragging) activeIndex else -1,
                        alphabet = alphabet,
                        currentLetters = currentLetters()
                    )

                    val defaultCenterX = if (isRightSide) {
                        size.width - 18.dp.toPx()
                    } else {
                        18.dp.toPx()
                    }

                    for (i in 0 until alphabet.size) {
                        val centerY = renderBuffer.centroids[i]
                        val displacement = renderBuffer.displacements[i]
                        val scale = renderBuffer.scales[i]
                        val alpha = renderBuffer.alphas[i]
                        val isDot = renderBuffer.isDot[i]
                        val isBold = renderBuffer.isBold[i]
                        val isActive = renderBuffer.isActive[i]

                        val color = if (isActive) primaryColor else defaultTextColor
                        val letterCenterX = defaultCenterX + displacement
                        val letterCenterY = centerY

                        scale(
                            scale = scale,
                            pivot = Offset(letterCenterX, letterCenterY)
                        ) {
                            if (alphabet[i] == "🔍") {
                                val iconSize = baseFontSize.toPx() * 1.15f
                                val topLeftX = letterCenterX - iconSize / 2f
                                val topLeftY = letterCenterY - iconSize / 2f
                                // Shadow pass
                                translate(left = topLeftX, top = topLeftY + 2f) {
                                    with(searchPainter) {
                                        draw(
                                            size = Size(iconSize, iconSize),
                                            alpha = alpha * 0.65f,
                                            colorFilter = ColorFilter.tint(Color.Black)
                                        )
                                    }
                                }
                                // Main icon pass
                                translate(left = topLeftX, top = topLeftY) {
                                    with(searchPainter) {
                                        draw(
                                            size = Size(iconSize, iconSize),
                                            alpha = alpha,
                                            colorFilter = ColorFilter.tint(color)
                                        )
                                    }
                                }
                            } else {
                                val layoutResult = if (isDot) {
                                    layoutCache.dotLayout
                                } else {
                                    layoutCache.getLayoutResult(i, isBold)
                                }

                                val textWidth = layoutResult.size.width
                                val textHeight = layoutResult.size.height
                                val topLeftX = letterCenterX - textWidth / 2f
                                val topLeftY = letterCenterY - textHeight / 2f

                                drawText(
                                    textLayoutResult = layoutResult,
                                    topLeft = Offset(topLeftX, topLeftY),
                                    color = color,
                                    alpha = alpha
                                )
                            }
                        }
                    }
                }
            }

            // 2. Full-height expanded touch target container along edge
            Box(
                modifier = Modifier
                    .align(if (isRightSide) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(SCROLLBAR_TOUCH_WIDTH_DP)
                    .pointerInput(alphabet, targetScrollbarHeight, isRightSide, verticalAlignment) {
                        try {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                val relativeTouchY = (down.position.y - railTopY).coerceIn(0f, railHeightPx)
                                touchY = relativeTouchY
                                isDragging = true

                                val touchWidthPx = size.width.toFloat()
                                val initialDistFromEdge = WavePhysicsEngine.calculateDistanceFromEdge(
                                    touchX = down.position.x,
                                    railWidthPx = touchWidthPx,
                                    isRightSide = isRightSide
                                )
                                targetDisplacementPx = WavePhysicsEngine.calculateDynamicDisplacement(
                                    distanceFromEdgePx = initialDistFromEdge,
                                    minDisplacementPx = minDisplacementPx,
                                    maxDisplacementPx = maxAllowedDisplacementPx,
                                    fingerClearancePx = fingerClearancePx
                                )

                                val initialIdx = WavePhysicsEngine.calculateIndexFromTouchY(
                                    touchY = relativeTouchY,
                                    totalHeight = railHeightPx,
                                    itemCount = alphabet.size
                                )

                                activeIndex = initialIdx
                                val initialLetter = alphabet[initialIdx]
                                lastActiveLetter = initialLetter
                                hapticManager.performScrollTick(currentHapticEnabled)
                                if (initialLetter !in ACTION_LETTERS) {
                                    currentOnLetterSelected(initialLetter)
                                }

                                var pointerReleasedNormally = false
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val primaryPointer = event.changes.firstOrNull { it.id == down.id }
                                        if (primaryPointer == null || !primaryPointer.pressed) {
                                            pointerReleasedNormally = true
                                            break
                                        }

                                        primaryPointer.consume()
                                        val newTouchY = (primaryPointer.position.y - railTopY).coerceIn(0f, railHeightPx)
                                        touchY = newTouchY

                                        val distFromEdge = WavePhysicsEngine.calculateDistanceFromEdge(
                                            touchX = primaryPointer.position.x,
                                            railWidthPx = touchWidthPx,
                                            isRightSide = isRightSide
                                        )
                                        targetDisplacementPx = WavePhysicsEngine.calculateDynamicDisplacement(
                                            distanceFromEdgePx = distFromEdge,
                                            minDisplacementPx = minDisplacementPx,
                                            maxDisplacementPx = maxAllowedDisplacementPx,
                                            fingerClearancePx = fingerClearancePx
                                        )

                                        val newIdx = WavePhysicsEngine.calculateIndexFromTouchY(
                                            touchY = newTouchY,
                                            totalHeight = railHeightPx,
                                            itemCount = alphabet.size
                                        )

                                        if (newIdx != activeIndex) {
                                            activeIndex = newIdx
                                            val newLetter = alphabet[newIdx]
                                            lastActiveLetter = newLetter
                                            hapticManager.performScrollTick(currentHapticEnabled)
                                            if (newLetter !in ACTION_LETTERS) {
                                                currentOnLetterSelected(newLetter)
                                            }
                                        }
                                    }
                                } finally {
                                    if (activeIndex in alphabet.indices) {
                                        val releasedLetter = alphabet[activeIndex]
                                        lastActiveLetter = releasedLetter
                                        if (pointerReleasedNormally && releasedLetter in ACTION_LETTERS) {
                                            currentOnLetterSelected(releasedLetter)
                                        }
                                    }
                                    isDragging = false
                                    activeIndex = -1
                                    targetDisplacementPx = defaultDisplacementPx
                                }
                            }
                        } finally {
                            if (activeIndex in alphabet.indices) {
                                lastActiveLetter = alphabet[activeIndex]
                            }
                            isDragging = false
                            activeIndex = -1
                            targetDisplacementPx = defaultDisplacementPx
                        }
                    }
            )

            // 3. Floating Bubble Indicator
            if (letterToShow.isNotEmpty() && (bubbleAlpha > 0.001f || isDragging)) {
                Surface(
                    modifier = Modifier
                        .size(bubbleSizeDp)
                        .graphicsLayer {
                            val defaultCenterX = if (isRightSide) {
                                railWidthPx - 18.dp.toPx()
                            } else {
                                18.dp.toPx()
                            }
                            val currentCrestDisplacementPx = WavePhysicsEngine.calculateDisplacement(
                                waveFactor = waveProgress,
                                maxDisplacementPx = animatedDisplacementPx.value,
                                isRightSide = isRightSide
                            )
                            val curBubbleX = WavePhysicsEngine.calculateBubbleX(
                                railLeftX = railLeftX,
                                defaultCenterX = defaultCenterX,
                                displacementPx = currentCrestDisplacementPx,
                                bubbleSizePx = bubbleSizePx,
                                bubbleGapPx = bubbleGapPx,
                                isRightSide = isRightSide,
                                totalViewportWidthPx = totalViewportWidthPx
                            )
                            val currentBubbleY = WavePhysicsEngine.calculateBubbleY(
                                touchY = animatedTouchY.value,
                                bubbleHeight = bubbleSizePx,
                                totalHeight = railHeightPx
                            )
                            translationX = curBubbleX
                            translationY = railTopY + currentBubbleY
                            alpha = bubbleAlpha
                            scaleX = bubbleScale
                            scaleY = bubbleScale
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (letterToShow == "⚙") {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(bubbleIconSizeDp)
                            )
                        } else if (letterToShow == "🔍") {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(bubbleIconSizeDp)
                            )
                        } else {
                            Text(
                                text = letterToShow,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = baseFontSize,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
