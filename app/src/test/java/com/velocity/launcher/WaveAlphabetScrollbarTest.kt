package com.velocity.launcher

import com.velocity.launcher.ui.compose.components.WaveAlphabetLayoutCache
import com.velocity.launcher.ui.compose.components.WavePhysicsEngine
import com.velocity.launcher.ui.compose.components.WaveRenderBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class WaveAlphabetScrollbarTest {

    private val delta = 0.0001f

    @Test
    fun testGaussianFactor_atCenter_returnsOne() {
        val factor = WavePhysicsEngine.calculateGaussianFactor(distance = 0f, sigma = 90f)
        assertEquals(1.0f, factor, delta)
    }

    @Test
    fun testGaussianFactor_atOneSigma_returnsExpectedDecay() {
        val sigma = 90f
        val factor = WavePhysicsEngine.calculateGaussianFactor(distance = sigma, sigma = sigma)
        val expected = exp(-0.5f) // ~0.60653
        assertEquals(expected, factor, delta)
    }

    @Test
    fun testGaussianFactor_atTwoSigma_returnsExpectedDecay() {
        val sigma = 90f
        val factor = WavePhysicsEngine.calculateGaussianFactor(distance = 2f * sigma, sigma = sigma)
        val expected = exp(-2.0f) // ~0.13533
        assertEquals(expected, factor, delta)
    }

    @Test
    fun testGaussianFactor_invalidSigma_returnsZero() {
        val factorZeroSigma = WavePhysicsEngine.calculateGaussianFactor(distance = 10f, sigma = 0f)
        val factorNegativeSigma = WavePhysicsEngine.calculateGaussianFactor(distance = 10f, sigma = -10f)
        assertEquals(0f, factorZeroSigma, delta)
        assertEquals(0f, factorNegativeSigma, delta)
    }

    @Test
    fun testWaveFactor_scaledByWaveProgress() {
        val distance = 0f
        val sigma = 90f

        val waveFactorZeroProgress = WavePhysicsEngine.calculateWaveFactor(distance, sigma, waveProgress = 0f)
        val waveFactorHalfProgress = WavePhysicsEngine.calculateWaveFactor(distance, sigma, waveProgress = 0.5f)
        val waveFactorFullProgress = WavePhysicsEngine.calculateWaveFactor(distance, sigma, waveProgress = 1.0f)

        assertEquals(0.0f, waveFactorZeroProgress, delta)
        assertEquals(0.5f, waveFactorHalfProgress, delta)
        assertEquals(1.0f, waveFactorFullProgress, delta)
    }

    @Test
    fun testDisplacement_rightSideDisplacesLeftward() {
        val maxDisplacement = 38f
        val displacementRight = WavePhysicsEngine.calculateDisplacement(
            waveFactor = 1.0f,
            maxDisplacementPx = maxDisplacement,
            isRightSide = true
        )
        // Right side moves inwards to the left (negative X)
        assertEquals(-38f, displacementRight, delta)
    }

    @Test
    fun testDisplacement_leftSideDisplacesRightward() {
        val maxDisplacement = 38f
        val displacementLeft = WavePhysicsEngine.calculateDisplacement(
            waveFactor = 1.0f,
            maxDisplacementPx = maxDisplacement,
            isRightSide = false
        )
        // Left side moves inwards to the right (positive X)
        assertEquals(38f, displacementLeft, delta)
    }

    @Test
    fun testScaleCalculation_inactiveAndActive() {
        // At rest
        val scaleRestInactive = WavePhysicsEngine.calculateScale(waveFactor = 0f, isActive = false)
        assertEquals(1.0f, scaleRestInactive, delta)

        val scaleRestActive = WavePhysicsEngine.calculateScale(waveFactor = 0f, isActive = true)
        assertEquals(1.2f, scaleRestActive, delta)

        // At peak wave
        val scalePeakInactive = WavePhysicsEngine.calculateScale(waveFactor = 1.0f, isActive = false)
        assertEquals(1.85f, scalePeakInactive, delta)

        val scalePeakActive = WavePhysicsEngine.calculateScale(waveFactor = 1.0f, isActive = true)
        assertEquals(1.85f * 1.2f, scalePeakActive, delta)
    }

    @Test
    fun testAlphaCalculation_states() {
        // Inactive at rest
        val alphaRest = WavePhysicsEngine.calculateAlpha(waveFactor = 0f, waveProgress = 0f, isActive = false, isDot = false)
        assertEquals(0.90f, alphaRest, delta)

        // Active letter is always 1.0f
        val alphaActive = WavePhysicsEngine.calculateAlpha(waveFactor = 0.5f, waveProgress = 1f, isActive = true, isDot = false)
        assertEquals(1.0f, alphaActive, delta)

        // Compact dot alpha is scaled down by 0.75
        val alphaDotRest = WavePhysicsEngine.calculateAlpha(waveFactor = 0f, waveProgress = 0f, isActive = false, isDot = true)
        assertEquals(0.90f * 0.75f, alphaDotRest, delta)

        // Wave active alpha
        val alphaWave = WavePhysicsEngine.calculateAlpha(waveFactor = 1.0f, waveProgress = 1.0f, isActive = false, isDot = false)
        assertEquals(1.0f, alphaWave, delta)
    }

    @Test
    fun testCalculateIndexFromTouchY_clampingAndResolution() {
        val totalHeight = 260f
        val itemCount = 26 // 10px per item

        // Normal ranges
        assertEquals(0, WavePhysicsEngine.calculateIndexFromTouchY(5f, totalHeight, itemCount))
        assertEquals(5, WavePhysicsEngine.calculateIndexFromTouchY(55f, totalHeight, itemCount))
        assertEquals(25, WavePhysicsEngine.calculateIndexFromTouchY(255f, totalHeight, itemCount))

        // Negative touchY clamped to 0
        assertEquals(0, WavePhysicsEngine.calculateIndexFromTouchY(-50f, totalHeight, itemCount))

        // Exceeding touchY clamped to lastIndex (25)
        assertEquals(25, WavePhysicsEngine.calculateIndexFromTouchY(400f, totalHeight, itemCount))

        // Zero / negative count or height edges
        assertEquals(0, WavePhysicsEngine.calculateIndexFromTouchY(100f, 0f, itemCount))
        assertEquals(0, WavePhysicsEngine.calculateIndexFromTouchY(100f, totalHeight, 0))
    }

    @Test
    fun testCentroidYCalculation() {
        val itemHeight = 20f
        assertEquals(10f, WavePhysicsEngine.calculateCentroidY(0, itemHeight), delta)
        assertEquals(30f, WavePhysicsEngine.calculateCentroidY(1, itemHeight), delta)
        assertEquals(50f, WavePhysicsEngine.calculateCentroidY(2, itemHeight), delta)
    }

    @Test
    fun testBubbleYClamping() {
        val bubbleHeight = 52f
        val totalHeight = 500f

        // Top clamp
        val topY = WavePhysicsEngine.calculateBubbleY(touchY = 10f, bubbleHeight = bubbleHeight, totalHeight = totalHeight)
        assertEquals(0f, topY, delta)

        // Centered
        val midY = WavePhysicsEngine.calculateBubbleY(touchY = 250f, bubbleHeight = bubbleHeight, totalHeight = totalHeight)
        assertEquals(250f - 26f, midY, delta)

        // Bottom clamp
        val bottomY = WavePhysicsEngine.calculateBubbleY(touchY = 550f, bubbleHeight = bubbleHeight, totalHeight = totalHeight)
        assertEquals(500f - 52f, bottomY, delta)
    }

    @Test
    fun testTargetTouchY_gestureIsolation() {
        val itemHeightPx = 20f

        // When dragging, targetTouchY must strictly follow touchY and ignore currentScrollIndex
        val targetWhileDragging = WavePhysicsEngine.calculateTargetTouchY(
            isDragging = true,
            touchY = 123.4f,
            currentScrollIndex = 5,
            itemHeightPx = itemHeightPx
        )
        assertEquals(123.4f, targetWhileDragging, delta)

        // When not dragging and scroll index is valid, targetTouchY aligns to currentScrollIndex centroid
        val targetRestingWithScroll = WavePhysicsEngine.calculateTargetTouchY(
            isDragging = false,
            touchY = 0f,
            currentScrollIndex = 3,
            itemHeightPx = itemHeightPx
        )
        assertEquals(3.5f * itemHeightPx, targetRestingWithScroll, delta)

        // When not dragging and no scroll index, fallback to touchY
        val targetRestingNoScroll = WavePhysicsEngine.calculateTargetTouchY(
            isDragging = false,
            touchY = 50f,
            currentScrollIndex = -1,
            itemHeightPx = itemHeightPx
        )
        assertEquals(50f, targetRestingNoScroll, delta)
    }

    @Test
    fun testDisplayedActiveIndex_gestureIsolation() {
        // While dragging with active wave
        assertEquals(
            5,
            WavePhysicsEngine.calculateDisplayedActiveIndex(
                isDragging = true,
                waveProgress = 0.8f,
                activeIndex = 5,
                currentScrollIndex = 2
            )
        )

        // While dragging before wave threshold (immediate active index)
        assertEquals(
            5,
            WavePhysicsEngine.calculateDisplayedActiveIndex(
                isDragging = true,
                waveProgress = 0.005f,
                activeIndex = 5,
                currentScrollIndex = 2
            )
        )

        // Not dragging: uses currentScrollIndex
        assertEquals(
            2,
            WavePhysicsEngine.calculateDisplayedActiveIndex(
                isDragging = false,
                waveProgress = 0f,
                activeIndex = 5,
                currentScrollIndex = 2
            )
        )
    }

    @Test
    fun testResolveLetterForScrollIndex_binarySearch() {
        val indices = intArrayOf(0, 1, 10, 25, 40)
        val letters = arrayOf("★", "A", "B", "C", "D")

        // Exact matches
        assertEquals("★", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 0))
        assertEquals("A", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 1))
        assertEquals("B", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 10))
        assertEquals("C", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 25))
        assertEquals("D", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 40))

        // In-between ranges (preceding anchor)
        assertEquals("A", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 5))
        assertEquals("B", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 15))
        assertEquals("C", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 39))
        assertEquals("D", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 100))

        // Empty indices / letters fallback
        assertNull(WavePhysicsEngine.resolveLetterForScrollIndex(intArrayOf(), arrayOf(), 5))
        assertEquals("FALLBACK", WavePhysicsEngine.resolveLetterForScrollIndex(intArrayOf(), arrayOf(), 5, "FALLBACK"))
    }

    @Test
    fun testComputeTransforms_populatesContiguousPrimitiveBuffers() {
        val alphabetSize = 5
        val buffer = WaveRenderBuffer(alphabetSize)
        val isAnchor = booleanArrayOf(true, false, false, false, true)

        val itemHeightPx = 20f
        val animatedTouchY = 10f // Centered on index 0
        val waveProgress = 1.0f
        val sigmaPx = 40f
        val maxDisplacementPx = 30f

        WavePhysicsEngine.computeTransforms(
            buffer = buffer,
            itemCount = alphabetSize,
            itemHeightPx = itemHeightPx,
            animatedTouchY = animatedTouchY,
            waveProgress = waveProgress,
            sigmaPx = sigmaPx,
            maxDisplacementPx = maxDisplacementPx,
            isRightSide = true,
            isCompact = true,
            isAnchor = isAnchor,
            activeIndex = 0
        )

        // Index 0: active and at peak
        assertEquals(10f, buffer.centroids[0], delta)
        assertEquals(-30f, buffer.displacements[0], delta) // Right side: negative displacement
        assertEquals(1.85f * 1.2f, buffer.scales[0], delta)
        assertEquals(1.0f, buffer.alphas[0], delta)
        assertTrue(buffer.isBold[0])
        assertFalse(buffer.isDot[0]) // Active is never dot

        // Centroids for remaining items
        assertEquals(30f, buffer.centroids[1], delta)
        assertEquals(50f, buffer.centroids[2], delta)
        assertEquals(70f, buffer.centroids[3], delta)
        assertEquals(90f, buffer.centroids[4], delta)

        // Far index 4 (dist = 80px = 2 sigma)
        val expectedWaveFactor4 = exp(-2.0f)
        assertEquals(-30f * expectedWaveFactor4, buffer.displacements[4], delta)
        assertEquals(1.0f + 0.85f * expectedWaveFactor4, buffer.scales[4], delta)
        assertFalse(buffer.isDot[4]) // Anchor is never dot
    }

    @Test
    fun testDefaultAnchorLetters_containsGearAndSearchIcon() {
        val defaultAnchors = com.velocity.launcher.ui.compose.components.DEFAULT_ANCHOR_LETTERS
        assertTrue(defaultAnchors.contains("★"))
        assertTrue(defaultAnchors.contains("A"))
        assertTrue(defaultAnchors.contains("E"))
        assertTrue(defaultAnchors.contains("I"))
        assertTrue(defaultAnchors.contains("M"))
        assertTrue(defaultAnchors.contains("Q"))
        assertTrue(defaultAnchors.contains("U"))
        assertTrue(defaultAnchors.contains("Z"))
        assertTrue(defaultAnchors.contains("#"))
        assertTrue(defaultAnchors.contains("⚙"))
        assertTrue(defaultAnchors.contains("🔍"))
    }

    @Test
    fun testResolveLetterForScrollIndex_withTrailingGearAndSearch() {
        val indices = intArrayOf(0, 1, 10, 25, 40, 45)
        val letters = arrayOf("★", "A", "M", "Z", "⚙", "🔍")

        assertEquals("★", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 0))
        assertEquals("A", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 5))
        assertEquals("M", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 10))
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 30))
        assertEquals("⚙", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 40))
        assertEquals("🔍", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 45))
        assertEquals("🔍", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 100))
    }

    @Test
    fun testResolveLetterForScrollIndex_singleAppSectionAtEndWithTrailingGear() {
        // Setup: Single app in 'Z' (header at index 25, single app at index 26, settings footer at index 27)
        val indices = intArrayOf(0, 1, 10, 25, 27)
        val letters = arrayOf("★", "A", "M", "Z", "⚙")

        // Exact header index for Z
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 25))
        // Single app item under Z
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 26))
        // Settings footer index
        assertEquals("⚙", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 27))
        // Overscroll past the settings footer
        assertEquals("⚙", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 28))
        assertEquals("⚙", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 100))
    }

    @Test
    fun testResolveLetterForScrollIndex_singleAppSectionAtEndWithoutTrailingIcon() {
        // Setup: Alphabet ending directly with 'Z' having a single app (header at 20, app at 21)
        val indices = intArrayOf(0, 5, 20)
        val letters = arrayOf("A", "B", "Z")

        // In preceding sections
        assertEquals("A", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 0))
        assertEquals("A", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 4))
        assertEquals("B", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 5))
        assertEquals("B", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 19))

        // Single app in 'Z'
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 20))
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 21))

        // Past last item index resolves to the last section 'Z'
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 22))
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(indices, letters, 50))
    }

    @Test
    fun testResolveLetterForScrollIndex_singleLetterAlphabetAndNegativeIndices() {
        val singleLetterIndices = intArrayOf(0)
        val singleLetters = arrayOf("Z")

        // Single letter alphabet tests
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(singleLetterIndices, singleLetters, 0))
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(singleLetterIndices, singleLetters, 1))
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(singleLetterIndices, singleLetters, 10))
        assertEquals("Z", WavePhysicsEngine.resolveLetterForScrollIndex(singleLetterIndices, singleLetters, -1))

        // Multi-letter negative index tests clamp to first letter
        val multiIndices = intArrayOf(0, 5, 10)
        val multiLetters = arrayOf("A", "B", "C")
        assertEquals("A", WavePhysicsEngine.resolveLetterForScrollIndex(multiIndices, multiLetters, -1))
        assertEquals("A", WavePhysicsEngine.resolveLetterForScrollIndex(multiIndices, multiLetters, -100))
    }

    @Test
    fun testResolveSectionIndex_behavior() {
        val indices = intArrayOf(0, 5, 15, 30)

        // Exact matches
        assertEquals(0, WavePhysicsEngine.resolveSectionIndex(indices, 0))
        assertEquals(1, WavePhysicsEngine.resolveSectionIndex(indices, 5))
        assertEquals(2, WavePhysicsEngine.resolveSectionIndex(indices, 15))
        assertEquals(3, WavePhysicsEngine.resolveSectionIndex(indices, 30))

        // In-between values
        assertEquals(0, WavePhysicsEngine.resolveSectionIndex(indices, 3))
        assertEquals(1, WavePhysicsEngine.resolveSectionIndex(indices, 10))
        assertEquals(2, WavePhysicsEngine.resolveSectionIndex(indices, 20))
        assertEquals(3, WavePhysicsEngine.resolveSectionIndex(indices, 50))

        // Negative values clamp to 0
        assertEquals(0, WavePhysicsEngine.resolveSectionIndex(indices, -5))

        // Empty array returns -1
        assertEquals(-1, WavePhysicsEngine.resolveSectionIndex(intArrayOf(), 5))
    }

    @Test
    fun testResolveVisibleLetters_singleAndMultipleSections() {
        val indices = intArrayOf(0, 5, 15, 30)
        val letters = arrayOf("★", "A", "B", "C")

        // Single visible item within first section
        assertEquals(setOf("★"), WavePhysicsEngine.resolveVisibleLetters(indices, letters, 0, 2))

        // Single visible item in middle section
        assertEquals(setOf("A"), WavePhysicsEngine.resolveVisibleLetters(indices, letters, 6, 10))

        // Span across 2 sections
        assertEquals(setOf("★", "A"), WavePhysicsEngine.resolveVisibleLetters(indices, letters, 2, 7))

        // Span across 3 sections
        assertEquals(setOf("★", "A", "B"), WavePhysicsEngine.resolveVisibleLetters(indices, letters, 0, 16))

        // Span across all sections
        assertEquals(setOf("★", "A", "B", "C"), WavePhysicsEngine.resolveVisibleLetters(indices, letters, 0, 40))

        // Empty indices or letters returns fallback
        assertEquals(setOf("FALLBACK"), WavePhysicsEngine.resolveVisibleLetters(intArrayOf(), arrayOf(), 0, 10, fallback = setOf("FALLBACK")))
    }

    @Test
    fun testComputeTransforms_multipleActiveItemsWhenNotDragging() {
        val alphabetSize = 4
        val buffer = WaveRenderBuffer(alphabetSize)
        val isAnchor = booleanArrayOf(true, false, false, true)
        val alphabet = listOf("★", "A", "B", "C")
        val currentLetters = setOf("★", "B")

        WavePhysicsEngine.computeTransforms(
            buffer = buffer,
            itemCount = alphabetSize,
            itemHeightPx = 20f,
            animatedTouchY = 0f,
            waveProgress = 0f,
            sigmaPx = 40f,
            maxDisplacementPx = 30f,
            isRightSide = true,
            isCompact = false,
            isAnchor = isAnchor,
            activeIndex = -1,
            alphabet = alphabet,
            currentLetters = currentLetters
        )

        // Items in currentLetters are active
        assertTrue(buffer.isActive[0]) // "★"
        assertFalse(buffer.isActive[1]) // "A"
        assertTrue(buffer.isActive[2]) // "B"
        assertFalse(buffer.isActive[3]) // "C"

        // Active items have scale 1.2f and alpha 1.0f at rest
        assertEquals(1.2f, buffer.scales[0], delta)
        assertEquals(1.0f, buffer.scales[1], delta)
        assertEquals(1.2f, buffer.scales[2], delta)
        assertEquals(1.0f, buffer.scales[3], delta)

        assertEquals(1.0f, buffer.alphas[0], delta)
        assertEquals(0.90f, buffer.alphas[1], delta)
        assertEquals(1.0f, buffer.alphas[2], delta)
        assertEquals(0.90f, buffer.alphas[3], delta)
    }

    @Test
    fun testComputeTransforms_singleActiveItemWhenDragging() {
        val alphabetSize = 4
        val buffer = WaveRenderBuffer(alphabetSize)
        val isAnchor = booleanArrayOf(true, false, false, true)
        val alphabet = listOf("★", "A", "B", "C")
        val currentLetters = setOf("★", "B") // When dragging, activeIndex overrides currentLetters

        WavePhysicsEngine.computeTransforms(
            buffer = buffer,
            itemCount = alphabetSize,
            itemHeightPx = 20f,
            animatedTouchY = 30f, // Touched on index 1 ("A")
            waveProgress = 1.0f,
            sigmaPx = 40f,
            maxDisplacementPx = 30f,
            isRightSide = true,
            isCompact = false,
            isAnchor = isAnchor,
            activeIndex = 1,
            alphabet = alphabet,
            currentLetters = currentLetters
        )

        // Only touched index 1 is active
        assertFalse(buffer.isActive[0])
        assertTrue(buffer.isActive[1])
        assertFalse(buffer.isActive[2])
        assertFalse(buffer.isActive[3])
    }

    @Test
    fun testRadialPhysicsEngine_breakoutAndCollapseThresholds() {
        val breakoutDp = com.velocity.launcher.ui.compose.components.RadialPhysicsEngine.BREAKOUT_THRESHOLD_DP
        val collapseDp = com.velocity.launcher.ui.compose.components.RadialPhysicsEngine.COLLAPSE_RAIL_THRESHOLD_DP
        val deadZoneDp = com.velocity.launcher.ui.compose.components.RadialPhysicsEngine.DEAD_ZONE_RADIUS_DP

        // Breakout must be significantly wider than collapse threshold to provide hysteresis
        assertTrue("Breakout threshold ($breakoutDp) should be greater than collapse threshold ($collapseDp)", breakoutDp > collapseDp)
        // Deadzone radius must be safe
        assertTrue("Deadzone radius ($deadZoneDp) should be at least 25dp", deadZoneDp >= 25f)
    }

    @Test
    fun testResolveFocalLetter_itemStartingAtExactFocalOffset_resolvesToItemLetterNotPreceding() {
        val indices = intArrayOf(0, 5, 10)
        val letters = arrayOf("A", "B", "C")
        // Item 4 (section A) is before, Item 5 (section B) starts at exact focal offset 100
        val visibleOffsets = listOf(
            4 to 50,
            5 to 100,
            6 to 150
        )
        val result = WavePhysicsEngine.resolveFocalLetter(indices, letters, visibleOffsets, focalOffsetPx = 100)
        assertEquals("B", result)
    }

    @Test
    fun testResolveFocalLetter_itemStartingAtFocalOffsetMinus4_resolvesToItemLetter() {
        val indices = intArrayOf(0, 5, 10)
        val letters = arrayOf("A", "B", "C")
        // Focal line at 100px. Item 5 starts at 96px (focalOffsetPx - 4), within the -8px margin.
        val visibleOffsets = listOf(
            4 to 40,
            5 to 96,
            6 to 150
        )
        val result = WavePhysicsEngine.resolveFocalLetter(indices, letters, visibleOffsets, focalOffsetPx = 100)
        assertEquals("B", result)
    }

    @Test
    fun testResolveFocalLetter_crossingFocalOffset() {
        val indices = intArrayOf(0, 5, 10)
        val letters = arrayOf("A", "B", "C")
        val visibleOffsets = listOf(
            0 to -100,
            4 to 50,
            5 to 120,
            9 to 180,
            10 to 250
        )

        // Focal line at 150px: first item with offset >= 142px is item 9 (offset 180) -> section 'B'
        val letterAt150 = WavePhysicsEngine.resolveFocalLetter(indices, letters, visibleOffsets, focalOffsetPx = 150)
        assertEquals("B", letterAt150)

        // Focal line at 120px: exact match for item 5 (offset 120 >= 112) -> section 'B'
        val letterAt120 = WavePhysicsEngine.resolveFocalLetter(indices, letters, visibleOffsets, focalOffsetPx = 120)
        assertEquals("B", letterAt120)

        // Focal line at 260px: item 10 (offset 250 < 252, but item 10 <= 260) -> section 'C'
        val letterAt260 = WavePhysicsEngine.resolveFocalLetter(indices, letters, visibleOffsets, focalOffsetPx = 260)
        assertEquals("C", letterAt260)
    }

    @Test
    fun testResolveFocalLetter_allVisibleItemsBelowFocalLine() {
        val indices = intArrayOf(0, 1, 5, 10)
        val letters = arrayOf("★", "A", "B", "C")
        val visibleOffsets = listOf(
            5 to 200,
            6 to 250,
            7 to 300
        )

        // All items have offset >= 100 - 8 = 92px -> first visible item is index 5 -> section 'B'
        val letter = WavePhysicsEngine.resolveFocalLetter(indices, letters, visibleOffsets, focalOffsetPx = 100)
        assertEquals("B", letter)
    }

    @Test
    fun testResolveFocalLetter_allVisibleItemsAboveFocalLine() {
        val indices = intArrayOf(0, 1, 5, 10)
        val letters = arrayOf("★", "A", "B", "C")
        val visibleOffsets = listOf(
            0 to -200,
            1 to -150,
            2 to -50
        )

        // All items have offset < 92px -> firstOrNull is null -> lastOrNull <= 100 picks item 2 (offset -50) -> section 'A'
        val letter = WavePhysicsEngine.resolveFocalLetter(indices, letters, visibleOffsets, focalOffsetPx = 100)
        assertEquals("A", letter)
    }

    @Test
    fun testResolveFocalLetter_emptyInputsAndFallbacks() {
        val indices = intArrayOf(0, 5)
        val letters = arrayOf("A", "B")

        // Empty visible offsets
        assertNull(WavePhysicsEngine.resolveFocalLetter(indices, letters, emptyList(), focalOffsetPx = 100))
        assertEquals("FB", WavePhysicsEngine.resolveFocalLetter(indices, letters, emptyList(), focalOffsetPx = 100, fallback = "FB"))

        // Empty indices or letters
        assertNull(WavePhysicsEngine.resolveFocalLetter(intArrayOf(), letters, listOf(0 to 50), focalOffsetPx = 100))
        assertNull(WavePhysicsEngine.resolveFocalLetter(indices, arrayOf(), listOf(0 to 50), focalOffsetPx = 100))
        assertEquals("FB", WavePhysicsEngine.resolveFocalLetter(intArrayOf(), arrayOf(), listOf(0 to 50), focalOffsetPx = 100, fallback = "FB"))
    }

    @Test
    fun testResolveFocalLetter_singleItemAndExactMatches() {
        val indices = intArrayOf(0, 5, 10)
        val letters = arrayOf("A", "B", "C")

        // Single item
        val singleOffset = listOf(10 to 100)
        assertEquals("C", WavePhysicsEngine.resolveFocalLetter(indices, letters, singleOffset, focalOffsetPx = 150))
        assertEquals("C", WavePhysicsEngine.resolveFocalLetter(indices, letters, singleOffset, focalOffsetPx = 50))

        // Exact match on offset
        val multipleOffsets = listOf(1 to 100, 5 to 200)
        assertEquals("B", WavePhysicsEngine.resolveFocalLetter(indices, letters, multipleOffsets, focalOffsetPx = 200))
    }

    @Test
    fun testFocalOffsetCalculation_clampingAndProportion() {
        fun computeFocalOffset(screenHeightDp: Float): Float {
            return (screenHeightDp * 0.35f).coerceIn(160f, 320f)
        }

        // Small screens clamp to min 160dp
        assertEquals(160f, computeFocalOffset(400f), delta)
        assertEquals(160f, computeFocalOffset(300f), delta)

        // Standard phone screens calculate 35% height
        assertEquals(245f, computeFocalOffset(700f), delta)
        assertEquals(280f, computeFocalOffset(800f), delta)

        // Large displays / tablets clamp to max 320dp
        assertEquals(320f, computeFocalOffset(1000f), delta)
        assertEquals(320f, computeFocalOffset(1200f), delta)

        // Trailing bottom spacer height is focalOffset + 80dp
        val focalOffset = computeFocalOffset(800f) // 280dp
        val bottomSpacerHeight = focalOffset + 80f
        assertEquals(360f, bottomSpacerHeight, delta)
    }

    @Test
    fun testIsolationVisibilityMatching_homeAndAlphabetSections() {
        fun isHomeVisible(isolatedLetter: String?): Boolean = isolatedLetter == null || isolatedLetter == "★"
        fun isSectionVisible(isolatedLetter: String?, sectionLetter: String): Boolean =
            isolatedLetter == null || isolatedLetter == sectionLetter

        // 1. Idle state (isolatedLetter == null): everything is visible
        assertTrue(isHomeVisible(null))
        assertTrue(isSectionVisible(null, "A"))
        assertTrue(isSectionVisible(null, "M"))
        assertTrue(isSectionVisible(null, "Z"))

        // 2. Star selected (isolatedLetter == "★"): Home is visible, alphabet sections are hidden
        assertTrue(isHomeVisible("★"))
        assertFalse(isSectionVisible("★", "A"))
        assertFalse(isSectionVisible("★", "M"))
        assertFalse(isSectionVisible("★", "Z"))

        // 3. Alphabet letter selected (e.g. "M"): Only section "M" is visible
        assertFalse(isHomeVisible("M"))
        assertFalse(isSectionVisible("M", "A"))
        assertTrue(isSectionVisible("M", "M"))
        assertFalse(isSectionVisible("M", "Z"))

        // 4. Another alphabet letter selected (e.g. "A"): Only section "A" is visible
        assertFalse(isHomeVisible("A"))
        assertTrue(isSectionVisible("A", "A"))
        assertFalse(isSectionVisible("A", "M"))
        assertFalse(isSectionVisible("A", "Z"))
    }

    @Test
    fun testListAlignment_scrollTargetAndOffsetCalculation() {
        data class ScrollTarget(val index: Int, val scrollOffset: Int)

        fun calculateScrollTarget(letter: String, letterToIndex: (String) -> Int?, focalOffsetPx: Int): ScrollTarget? {
            return when (letter) {
                "🔍", "⚙" -> null // Action icons do not scroll the main list
                "★" -> ScrollTarget(index = 0, scrollOffset = 0)
                else -> {
                    val index = letterToIndex(letter) ?: return null
                    ScrollTarget(index = index, scrollOffset = -focalOffsetPx)
                }
            }
        }

        val letterIndexMap = mapOf(
            "A" to 1,
            "B" to 10,
            "M" to 25,
            "Z" to 50
        )
        val focalOffsetPx = 420

        // Favorites / Top of list
        val starTarget = calculateScrollTarget("★", letterIndexMap::get, focalOffsetPx)
        assertEquals(ScrollTarget(index = 0, scrollOffset = 0), starTarget)

        // Alphabet letter scrolling with negative focal offset px
        val aTarget = calculateScrollTarget("A", letterIndexMap::get, focalOffsetPx)
        assertEquals(ScrollTarget(index = 1, scrollOffset = -420), aTarget)

        val mTarget = calculateScrollTarget("M", letterIndexMap::get, focalOffsetPx)
        assertEquals(ScrollTarget(index = 25, scrollOffset = -420), mTarget)

        val zTarget = calculateScrollTarget("Z", letterIndexMap::get, focalOffsetPx)
        assertEquals(ScrollTarget(index = 50, scrollOffset = -420), zTarget)

        // Search and Settings action icons return null (handled by overlay/settings navigation)
        assertNull(calculateScrollTarget("🔍", letterIndexMap::get, focalOffsetPx))
        assertNull(calculateScrollTarget("⚙", letterIndexMap::get, focalOffsetPx))

        // Unknown letter returns null
        assertNull(calculateScrollTarget("X", letterIndexMap::get, focalOffsetPx))
    }
}

