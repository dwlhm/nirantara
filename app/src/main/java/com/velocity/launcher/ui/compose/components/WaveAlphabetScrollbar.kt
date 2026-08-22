package com.velocity.launcher.ui.compose.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.ScrollbarPosition
import com.velocity.launcher.data.ScrollbarVerticalAlignment
import com.velocity.launcher.ui.theme.AppLabelFontSize
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

val DEFAULT_ANCHOR_LETTERS: Set<String> = setOf("★", "A", "E", "I", "M", "Q", "U", "Z", "#", "🔍", "⚙")

enum class ScrollbarGestureState {
    IDLE,
    SCRUBBING,
    RADIAL_EXPANDED
}

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

/**
 * Layout and text measurement cache (OOP) to pre-measure glyphs and anchor flags.
 */
class WaveAlphabetLayoutCache(
    val alphabet: List<String>,
    val baseFontSize: TextUnit = AppLabelFontSize,
    textMeasurer: TextMeasurer,
    anchorLetters: Set<String> = DEFAULT_ANCHOR_LETTERS
) {
    val size: Int = alphabet.size
    val isAnchor: BooleanArray = BooleanArray(size) { index ->
        val letter = alphabet[index]
        letter in anchorLetters || index == 0 || index == alphabet.lastIndex
    }

    private val normalLayouts: Array<TextLayoutResult>
    private val boldLayouts: Array<TextLayoutResult>
    val dotLayout: TextLayoutResult

    init {
        val letterShadow = Shadow(
            color = Color.Black.copy(alpha = 0.65f),
            offset = Offset(0f, 2f),
            blurRadius = 4f
        )
        val normalStyle = TextStyle(
            fontSize = baseFontSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            shadow = letterShadow
        )
        val anchorStyle = TextStyle(
            fontSize = baseFontSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            shadow = letterShadow
        )
        val boldStyle = TextStyle(
            fontSize = baseFontSize,
            fontWeight = FontWeight.ExtraBold,
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

    fun calculateScale(waveFactor: Float, isActive: Boolean): Float {
        val baseScale = 1.0f + 0.85f * waveFactor
        return if (isActive) baseScale * 1.2f else baseScale
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
        currentLetters: Set<String>? = null
    ) {
        val count = itemCount.coerceAtMost(buffer.capacity)
        for (i in 0 until count) {
            val centerY = (i + 0.5f) * itemHeightPx
            buffer.centroids[i] = centerY

            val dist = abs(animatedTouchY - centerY)
            val rawWave = calculateGaussianFactor(dist, sigmaPx)
            val waveFactor = rawWave * waveProgress

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
            buffer.isBold[i] = isActive || waveFactor >= 0.25f
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
    getRadialAppsForLetter: (String) -> List<AppModel> = { emptyList() },
    getIconBitmap: (AppModel) -> ImageBitmap? = { null },
    onLaunchApp: (AppModel) -> Unit = {},
    onOpenRadialEditor: (String) -> Unit = {},
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
                getRadialAppsForLetter = getRadialAppsForLetter,
                getIconBitmap = getIconBitmap,
                onLaunchApp = onLaunchApp,
                onOpenRadialEditor = onOpenRadialEditor,
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
                getRadialAppsForLetter = getRadialAppsForLetter,
                getIconBitmap = getIconBitmap,
                onLaunchApp = onLaunchApp,
                onOpenRadialEditor = onOpenRadialEditor,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SingleSideWaveScrollbar(
    alphabet: List<String>,
    currentLetters: Set<String>,
    focusedLetter: String? = null,
    targetScrollbarHeight: Dp,
    isRightSide: Boolean,
    verticalAlignment: ScrollbarVerticalAlignment,
    hapticEnabled: Boolean,
    hapticManager: HapticFeedbackManager,
    onLetterSelected: (String) -> Unit,
    getRadialAppsForLetter: (String) -> List<AppModel>,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    onLaunchApp: (AppModel) -> Unit,
    onOpenRadialEditor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var gestureState by remember { mutableStateOf(ScrollbarGestureState.IDLE) }
    val isDragging = gestureState != ScrollbarGestureState.IDLE
    val isRadialExpanded = gestureState == ScrollbarGestureState.RADIAL_EXPANDED

    var activeRadialTargetIndex by remember { mutableIntStateOf(RadialPhysicsEngine.TARGET_NONE) }

    var touchY by remember { mutableFloatStateOf(0f) }
    var pointerOffsetX by remember { mutableFloatStateOf(0f) }
    var pointerOffsetY by remember { mutableFloatStateOf(0f) }
    var activeIndex by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current

    val currentOnLetterSelected by rememberUpdatedState(onLetterSelected)
    val currentGetRadialApps by rememberUpdatedState(getRadialAppsForLetter)
    val currentOnLaunchApp by rememberUpdatedState(onLaunchApp)
    val currentOnOpenRadialEditor by rememberUpdatedState(onOpenRadialEditor)
    val currentHapticEnabled by rememberUpdatedState(hapticEnabled)

    val focusedIndex = remember(alphabet, focusedLetter, currentLetters) {
        if (focusedLetter != null) {
            val idx = alphabet.indexOf(focusedLetter)
            if (idx >= 0) idx else alphabet.indexOfFirst { it in currentLetters }
        } else {
            alphabet.indexOfFirst { it in currentLetters }
        }
    }

    val activeLetter = if (isDragging && activeIndex in alphabet.indices) alphabet[activeIndex] else null
    var lastActiveLetter by remember { mutableStateOf("") }
    if (activeLetter != null) {
        lastActiveLetter = activeLetter
    }
    val letterToShow = activeLetter ?: lastActiveLetter

    val radialApps = remember(letterToShow, currentGetRadialApps) {
        if (letterToShow.isNotEmpty()) currentGetRadialApps(letterToShow) else emptyList()
    }

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

        val sigmaPx = with(density) { 90.dp.toPx() }
        val maxDisplacementPx = with(density) { 38.dp.toPx() }
        val breakoutThresholdPx = with(density) { RadialPhysicsEngine.BREAKOUT_THRESHOLD_DP.dp.toPx() }
        val collapseRailPx = with(density) { RadialPhysicsEngine.COLLAPSE_RAIL_THRESHOLD_DP.dp.toPx() }
        val maxSelectionPx = with(density) { RadialPhysicsEngine.MAX_SELECTION_RADIUS_DP.dp.toPx() }
        val bubbleDeadZonePx = with(density) { RadialPhysicsEngine.DEAD_ZONE_RADIUS_DP.dp.toPx() }
        val editActivationPx = with(density) { RadialPhysicsEngine.EDIT_ACTIVATION_RADIUS_DP.dp.toPx() }
        val arcRadiusPx = with(density) { 100.dp.toPx() }
        val nodeHitRadiusPx = with(density) { 32.dp.toPx() }

        val animatedTouchY = remember { Animatable(0f) }

        LaunchedEffect(focusedIndex, isDragging, itemHeightPx) {
            if (!isDragging && focusedIndex >= 0 && itemHeightPx > 0f) {
                animatedTouchY.animateTo(
                    targetValue = (focusedIndex + 0.5f) * itemHeightPx,
                    animationSpec = spring(
                        stiffness = 1200f,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            }
        }

        val waveProgress by animateFloatAsState(
            targetValue = if (isDragging) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
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

        val bubbleSizeDp = 52.dp
        val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }

        val bubbleAlpha by animateFloatAsState(
            targetValue = if (isDragging && activeLetter != null) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "bubbleAlpha"
        )

        val bubbleScale by animateFloatAsState(
            targetValue = if (isDragging && activeLetter != null) 1f else 0.7f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "bubbleScale"
        )

        val radialArcAlpha by animateFloatAsState(
            targetValue = if (isRadialExpanded && isDragging) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "radialArcAlpha"
        )

        val radialArcScale by animateFloatAsState(
            targetValue = if (isRadialExpanded && isDragging) 1f else 0.5f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "radialArcScale"
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
                    .pointerInput(alphabet, targetScrollbarHeight, isRightSide, verticalAlignment) {
                        coroutineScope {
                            val touchChannel = Channel<Float>(Channel.CONFLATED)
                            val snapJob = launch {
                                for (targetY in touchChannel) {
                                    animatedTouchY.snapTo(targetY)
                                }
                            }
                            try {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    gestureState = ScrollbarGestureState.SCRUBBING
                                    activeRadialTargetIndex = RadialPhysicsEngine.TARGET_NONE

                                    val railTouchX = down.position.x
                                    var primaryPointerId = down.id
                                    var secondaryPointerId: androidx.compose.ui.input.pointer.PointerId? = null

                                    val relativeTouchY = down.position.y.coerceIn(0f, railHeightPx)
                                    touchY = relativeTouchY
                                    touchChannel.trySend(relativeTouchY)
                                    var lastScrubY = relativeTouchY
                                    var lastScrubTime = System.currentTimeMillis()
                                    var letterDwellStartTime = System.currentTimeMillis()

                                    val initialIdx = WavePhysicsEngine.calculateIndexFromTouchY(
                                        touchY = relativeTouchY,
                                        totalHeight = railHeightPx,
                                        itemCount = alphabet.size
                                    )

                                    activeIndex = initialIdx
                                    hapticManager.performScrollTick(currentHapticEnabled)
                                    currentOnLetterSelected(alphabet[initialIdx])

                                    val dwellJob = launch {
                                        while (gestureState != ScrollbarGestureState.IDLE) {
                                            delay(16L)
                                            val now = System.currentTimeMillis()

                                            if (gestureState == ScrollbarGestureState.SCRUBBING && letterDwellStartTime > 0L) {
                                                val elapsed = now - letterDwellStartTime
                                                val curLetter = alphabet.getOrElse(activeIndex) { "" }
                                                if (elapsed >= RadialPhysicsEngine.DWELL_EXPANSION_MS && curLetter != "⚙" && curLetter != "🔍") {
                                                    gestureState = ScrollbarGestureState.RADIAL_EXPANDED
                                                    hapticManager.performSpringPop(currentHapticEnabled)
                                                    activeRadialTargetIndex = RadialPhysicsEngine.TARGET_NONE
                                                }
                                            }
                                        }
                                    }

                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()

                                            val curLetter = alphabet.getOrElse(activeIndex) { if (letterToShow.isNotEmpty()) letterToShow else alphabet.firstOrNull() ?: "" }
                                            val curRadialApps = if (curLetter.isNotEmpty()) currentGetRadialApps(curLetter) else emptyList()

                                            // Multi-touch: check for second finger touching down on nodes
                                            if (gestureState == ScrollbarGestureState.RADIAL_EXPANDED) {
                                                val newDown = event.changes.firstOrNull { it.id != primaryPointerId && it.pressed && !it.isConsumed }
                                                if (newDown != null && secondaryPointerId == null) {
                                                    val curBubbleRelX = if (isRightSide) -76.dp.toPx() else 56.dp.toPx()
                                                    val curBubbleY = WavePhysicsEngine.calculateBubbleY(
                                                        touchY = touchY,
                                                        bubbleHeight = bubbleSizePx,
                                                        totalHeight = railHeightPx
                                                    )
                                                    val curBubbleCenterY = curBubbleY + bubbleSizePx / 2f
                                                    val curBubbleCenterX = curBubbleRelX + bubbleSizePx / 2f
                                                    val touchYRatio = if (railHeightPx > 0f) (curBubbleY / railHeightPx).coerceIn(0f, 1f) else 0f

                                                    val directNode = RadialPhysicsEngine.findTappedNode(
                                                        displacementX = newDown.position.x - curBubbleCenterX,
                                                        displacementY = newDown.position.y - curBubbleCenterY,
                                                        appCount = curRadialApps.size,
                                                        isRightSide = isRightSide,
                                                        touchYRatio = touchYRatio,
                                                        arcRadiusPx = arcRadiusPx,
                                                        hitRadiusPx = nodeHitRadiusPx
                                                    )

                                                    if (directNode != RadialPhysicsEngine.TARGET_NONE) {
                                                        newDown.consume()
                                                        secondaryPointerId = newDown.id
                                                        activeRadialTargetIndex = directNode
                                                        hapticManager.performSnapTick(currentHapticEnabled)
                                                    }
                                                }
                                            }

                                            val activeDrivingPointerId = secondaryPointerId ?: primaryPointerId
                                            val drivingPointer = event.changes.firstOrNull { it.id == activeDrivingPointerId }
                                            val primaryPointer = event.changes.firstOrNull { it.id == primaryPointerId }

                                            if (secondaryPointerId != null) {
                                                val secPointer = event.changes.firstOrNull { it.id == secondaryPointerId }
                                                if (secPointer == null || !secPointer.pressed) {
                                                    // Secondary finger released
                                                    if (activeRadialTargetIndex == RadialPhysicsEngine.EDIT_NODE_INDEX) {
                                                        hapticManager.performSnapTick(currentHapticEnabled)
                                                        currentOnOpenRadialEditor(curLetter)
                                                    } else if (activeRadialTargetIndex in curRadialApps.indices) {
                                                        hapticManager.performLaunchHeavyBuzz(currentHapticEnabled)
                                                        currentOnLaunchApp(curRadialApps[activeRadialTargetIndex])
                                                    }

                                                    secondaryPointerId = null
                                                    gestureState = ScrollbarGestureState.IDLE
                                                    activeRadialTargetIndex = RadialPhysicsEngine.TARGET_NONE
                                                    break
                                                }
                                            }

                                            if (primaryPointer == null || !primaryPointer.pressed) {
                                                // Primary finger released
                                                when (gestureState) {
                                                    ScrollbarGestureState.RADIAL_EXPANDED -> {
                                                        if (activeRadialTargetIndex == RadialPhysicsEngine.EDIT_NODE_INDEX) {
                                                            hapticManager.performSnapTick(currentHapticEnabled)
                                                            currentOnOpenRadialEditor(curLetter)
                                                        } else if (activeRadialTargetIndex in curRadialApps.indices) {
                                                            hapticManager.performLaunchHeavyBuzz(currentHapticEnabled)
                                                            currentOnLaunchApp(curRadialApps[activeRadialTargetIndex])
                                                        }
                                                    }
                                                    else -> {}
                                                }

                                                gestureState = ScrollbarGestureState.IDLE
                                                activeRadialTargetIndex = RadialPhysicsEngine.TARGET_NONE
                                                break
                                            }

                                            val currentPointer = drivingPointer ?: primaryPointer
                                            currentPointer.consume()

                                            val currentX = currentPointer.position.x
                                            val currentY = currentPointer.position.y
                                            pointerOffsetX = currentX
                                            pointerOffsetY = currentY

                                            val curBubbleRelX = if (isRightSide) -76.dp.toPx() else 56.dp.toPx()
                                            val curBubbleY = WavePhysicsEngine.calculateBubbleY(
                                                touchY = touchY,
                                                bubbleHeight = bubbleSizePx,
                                                totalHeight = railHeightPx
                                            )
                                            val curBubbleCenterY = curBubbleY + bubbleSizePx / 2f
                                            val curBubbleCenterX = curBubbleRelX + bubbleSizePx / 2f
                                            val dispFromBubbleX = currentX - curBubbleCenterX
                                            val dispFromBubbleY = currentY - curBubbleCenterY
                                            val distToBubble = hypot(dispFromBubbleX, dispFromBubbleY)

                                            val touchYRatio = if (railHeightPx > 0f) (curBubbleY / railHeightPx).coerceIn(0f, 1f) else 0f

                                            val inwardDistance = if (isRightSide) {
                                                railTouchX - currentX
                                            } else {
                                                currentX - railTouchX
                                            }

                                            if (gestureState == ScrollbarGestureState.SCRUBBING) {
                                                val newTouchY = currentY.coerceIn(0f, railHeightPx)
                                                touchY = newTouchY
                                                touchChannel.trySend(newTouchY)

                                                val newIdx = WavePhysicsEngine.calculateIndexFromTouchY(
                                                    touchY = newTouchY,
                                                    totalHeight = railHeightPx,
                                                    itemCount = alphabet.size
                                                )

                                                val scrubDeltaY = abs(newTouchY - lastScrubY)
                                                val timeDelta = (System.currentTimeMillis() - lastScrubTime).coerceAtLeast(1L)
                                                val speed = scrubDeltaY / timeDelta

                                                if (newIdx != activeIndex) {
                                                    activeIndex = newIdx
                                                    letterDwellStartTime = System.currentTimeMillis()
                                                    hapticManager.performScrollTick(currentHapticEnabled)
                                                    currentOnLetterSelected(alphabet[newIdx])
                                                } else if (speed > 0.05f) {
                                                    letterDwellStartTime = System.currentTimeMillis()
                                                }

                                                lastScrubY = newTouchY
                                                lastScrubTime = System.currentTimeMillis()

                                                val curLetterScrub = alphabet.getOrElse(activeIndex) { "" }
                                                val canExpandRadial = curLetterScrub != "⚙" && curLetterScrub != "🔍"
                                                if (canExpandRadial && inwardDistance >= breakoutThresholdPx) {
                                                    gestureState = ScrollbarGestureState.RADIAL_EXPANDED
                                                    hapticManager.performSpringPop(currentHapticEnabled)
                                                    activeRadialTargetIndex = RadialPhysicsEngine.TARGET_NONE
                                                }
                                            } else if (gestureState == ScrollbarGestureState.RADIAL_EXPANDED) {
                                                if (secondaryPointerId == null && inwardDistance < collapseRailPx) {
                                                    gestureState = ScrollbarGestureState.SCRUBBING
                                                    activeRadialTargetIndex = RadialPhysicsEngine.TARGET_NONE
                                                    val relY = currentY.coerceIn(0f, railHeightPx)
                                                    touchY = relY
                                                    touchChannel.trySend(relY)
                                                    letterDwellStartTime = System.currentTimeMillis()
                                                    lastScrubY = relY
                                                    lastScrubTime = System.currentTimeMillis()
                                                } else if (distToBubble <= bubbleDeadZonePx) {
                                                    if (activeRadialTargetIndex != RadialPhysicsEngine.TARGET_NONE) {
                                                        hapticManager.performSnapTick(currentHapticEnabled)
                                                        activeRadialTargetIndex = RadialPhysicsEngine.TARGET_NONE
                                                    }
                                                } else {
                                                    val resolvedTarget = RadialPhysicsEngine.resolveTargetIndex(
                                                        displacementX = dispFromBubbleX,
                                                        displacementY = dispFromBubbleY,
                                                        appCount = curRadialApps.size,
                                                        isRightSide = isRightSide,
                                                        touchYRatio = touchYRatio,
                                                        deadZoneRadius = bubbleDeadZonePx,
                                                        editActivationRadius = editActivationPx,
                                                        maxSelectionRadius = maxSelectionPx,
                                                        previousTargetIndex = activeRadialTargetIndex,
                                                        arcRadiusPx = arcRadiusPx,
                                                        nodeHitRadiusPx = nodeHitRadiusPx
                                                    )

                                                    if (resolvedTarget != activeRadialTargetIndex) {
                                                        val prevTarget = activeRadialTargetIndex
                                                        activeRadialTargetIndex = resolvedTarget
                                                        if (resolvedTarget != RadialPhysicsEngine.TARGET_NONE && prevTarget != resolvedTarget) {
                                                            hapticManager.performSnapTick(currentHapticEnabled)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } finally {
                                        dwellJob.cancel()
                                    }
                                }
                            } finally {
                                snapJob.cancel()
                            }
                        }
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
                        maxDisplacementPx = maxDisplacementPx,
                        isRightSide = isRightSide,
                        isCompact = isCompact,
                        isAnchor = layoutCache.isAnchor,
                        activeIndex = if (isDragging) activeIndex else -1,
                        alphabet = alphabet,
                        currentLetters = currentLetters
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

            // 2. Floating Bubble Indicator
            if (letterToShow.isNotEmpty() && (bubbleAlpha > 0.001f || isDragging)) {
                Surface(
                    modifier = Modifier
                        .size(bubbleSizeDp)
                        .graphicsLayer {
                            val curBubbleX = if (isRightSide) {
                                railLeftX - 76.dp.toPx()
                            } else {
                                railLeftX + 56.dp.toPx()
                            }
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
                    border = if (isRadialExpanded && activeRadialTargetIndex == RadialPhysicsEngine.TARGET_NONE) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    } else null,
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (letterToShow == "⚙") {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        } else if (letterToShow == "🔍") {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Text(
                                text = letterToShow,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // 3. Radial Morphing Arc Nodes (Apps + Outlying Edit Node)
                if (radialArcAlpha > 0.001f || isRadialExpanded) {
                    // Render Radial App Nodes
                    for (i in radialApps.indices) {
                        val app = radialApps[i]
                        val isHovered = activeRadialTargetIndex == i
                        val bmp = getIconBitmap(app)

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    val curBubbleX = if (isRightSide) {
                                        railLeftX - 76.dp.toPx()
                                    } else {
                                        railLeftX + 56.dp.toPx()
                                    }
                                    val currentBubbleY = WavePhysicsEngine.calculateBubbleY(
                                        touchY = animatedTouchY.value,
                                        bubbleHeight = bubbleSizePx,
                                        totalHeight = railHeightPx
                                    )
                                    val touchYRatio = if (railHeightPx > 0f) (currentBubbleY / railHeightPx).coerceIn(0f, 1f) else 0f
                                    val (offsetX, offsetY) = RadialPhysicsEngine.calculateNodeOffset(
                                        index = i,
                                        totalApps = radialApps.size,
                                        radiusPx = arcRadiusPx * radialArcScale,
                                        isRightSide = isRightSide,
                                        touchYRatio = touchYRatio
                                    )
                                    val nodeScale = if (isHovered) 1.25f else 1.0f

                                    translationX = curBubbleX + offsetX + (bubbleSizePx - 44.dp.toPx()) / 2f
                                    translationY = railTopY + currentBubbleY + offsetY + (bubbleSizePx - 44.dp.toPx()) / 2f
                                    scaleX = nodeScale * radialArcScale
                                    scaleY = nodeScale * radialArcScale
                                    alpha = radialArcAlpha
                                }
                                .size(44.dp)
                                .shadow(if (isHovered) 12.dp else 4.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (isHovered) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = if (isHovered) 2.5.dp else 1.dp,
                                    color = if (isHovered) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Text(
                                    text = app.label.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Hovered Tooltip Label
                        if (isHovered && radialArcAlpha > 0.5f) {
                            Surface(
                                modifier = Modifier
                                    .graphicsLayer {
                                        val curBubbleX = if (isRightSide) {
                                            railLeftX - 76.dp.toPx()
                                        } else {
                                            railLeftX + 56.dp.toPx()
                                        }
                                        val currentBubbleY = WavePhysicsEngine.calculateBubbleY(
                                            touchY = animatedTouchY.value,
                                            bubbleHeight = bubbleSizePx,
                                            totalHeight = railHeightPx
                                        )
                                        val touchYRatio = if (railHeightPx > 0f) (currentBubbleY / railHeightPx).coerceIn(0f, 1f) else 0f
                                        val (offsetX, offsetY) = RadialPhysicsEngine.calculateNodeOffset(
                                            index = i,
                                            totalApps = radialApps.size,
                                            radiusPx = arcRadiusPx * radialArcScale,
                                            isRightSide = isRightSide,
                                            touchYRatio = touchYRatio
                                        )
                                        val labelWidthEst = 80.dp.toPx()
                                        translationX = curBubbleX + offsetX + (bubbleSizePx - labelWidthEst) / 2f + (if (isRightSide) -24.dp.toPx() else 24.dp.toPx())
                                        translationY = railTopY + currentBubbleY + offsetY - 28.dp.toPx()
                                        alpha = radialArcAlpha
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.inverseSurface,
                                shadowElevation = 6.dp
                            ) {
                                Text(
                                    text = app.label,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Render Outlying Edit Node (✏️)
                    val isEditHovered = activeRadialTargetIndex == RadialPhysicsEngine.EDIT_NODE_INDEX

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val curBubbleX = if (isRightSide) {
                                    railLeftX - 76.dp.toPx()
                                } else {
                                    railLeftX + 56.dp.toPx()
                                }
                                val currentBubbleY = WavePhysicsEngine.calculateBubbleY(
                                    touchY = animatedTouchY.value,
                                    bubbleHeight = bubbleSizePx,
                                    totalHeight = railHeightPx
                                )
                                val touchYRatio = if (railHeightPx > 0f) (currentBubbleY / railHeightPx).coerceIn(0f, 1f) else 0f
                                val (editOffsetX, editOffsetY) = RadialPhysicsEngine.calculateEditNodeOffset(
                                    radiusPx = arcRadiusPx * 0.95f * radialArcScale,
                                    isRightSide = isRightSide,
                                    touchYRatio = touchYRatio
                                )
                                val editScale = if (isEditHovered) 1.20f else 1.0f

                                translationX = curBubbleX + editOffsetX + (bubbleSizePx - 38.dp.toPx()) / 2f
                                translationY = railTopY + currentBubbleY + editOffsetY + (bubbleSizePx - 38.dp.toPx()) / 2f
                                scaleX = editScale * radialArcScale
                                scaleY = editScale * radialArcScale
                                alpha = radialArcAlpha
                            }
                            .size(38.dp)
                            .shadow(if (isEditHovered) 10.dp else 3.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(
                                if (isEditHovered) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isEditHovered) 2.dp else 1.dp,
                                color = if (isEditHovered) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Radial Arc",
                            tint = if (isEditHovered) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isEditHovered && radialArcAlpha > 0.5f) {
                        Surface(
                            modifier = Modifier
                                .graphicsLayer {
                                    val curBubbleX = if (isRightSide) {
                                        railLeftX - 76.dp.toPx()
                                    } else {
                                        railLeftX + 56.dp.toPx()
                                    }
                                    val currentBubbleY = WavePhysicsEngine.calculateBubbleY(
                                        touchY = animatedTouchY.value,
                                        bubbleHeight = bubbleSizePx,
                                        totalHeight = railHeightPx
                                    )
                                    val touchYRatio = if (railHeightPx > 0f) (currentBubbleY / railHeightPx).coerceIn(0f, 1f) else 0f
                                    val (editOffsetX, editOffsetY) = RadialPhysicsEngine.calculateEditNodeOffset(
                                        radiusPx = arcRadiusPx * 0.95f * radialArcScale,
                                        isRightSide = isRightSide,
                                        touchYRatio = touchYRatio
                                    )
                                    val labelWidthEst = 70.dp.toPx()
                                    translationX = curBubbleX + editOffsetX + (bubbleSizePx - labelWidthEst) / 2f + (if (isRightSide) -24.dp.toPx() else 24.dp.toPx())
                                    translationY = railTopY + currentBubbleY + editOffsetY - 26.dp.toPx()
                                    alpha = radialArcAlpha
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.inverseSurface,
                            shadowElevation = 6.dp
                        ) {
                            Text(
                                text = "Edit Arc",
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
