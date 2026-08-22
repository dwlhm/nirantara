package com.velocity.launcher.ui.compose.components

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object RadialPhysicsEngine {
    const val BREAKOUT_THRESHOLD_DP = 26f
    const val COLLAPSE_RAIL_THRESHOLD_DP = 16f
    const val DWELL_EXPANSION_MS = 320L
    const val HOVER_CONFIRMATION_MS = 75L
    const val DEAD_ZONE_RADIUS_DP = 28f
    const val APP_ACTIVATION_RADIUS_DP = 36f
    const val EDIT_ACTIVATION_RADIUS_DP = 48f
    const val MAX_SELECTION_RADIUS_DP = 140f
    const val EDIT_NODE_INDEX = -2
    const val TARGET_NONE = -1
    const val TOTAL_APP_SWEEP = 104f
    const val EDIT_START_OFFSET = 120f
    const val EDIT_SWEEP = 35f
    const val HYSTERESIS_SECTOR_RATIO = 0.18f

    /**
     * Calculates the base start angle in degrees based on the vertical touch position ratio.
     * Smoothly interpolates from top edge (0°) through center (-52°) to bottom edge (-80°),
     * preventing sudden discontinuous angular jumps.
     */
    fun calculateBaseStartAngle(touchYRatio: Float): Float {
        val clampedRatio = touchYRatio.coerceIn(0f, 1f)
        return if (clampedRatio <= 0.5f) {
            // Smoothly interpolate from 0° at top (0.0) to -52° at center (0.5)
            val t = clampedRatio / 0.5f
            0f + t * -52f
        } else {
            // Smoothly interpolate from -52° at center (0.5) to -80° at bottom (1.0)
            val t = (clampedRatio - 0.5f) / 0.5f
            -52f + t * (-80f - (-52f))
        }
    }

    /**
     * Resolves the active radial target index (0..N-1 for apps, -2 for Edit node, -1 for deadzone / none).
     *
     * Features:
     * - Central deadzone: returns TARGET_NONE (-1) when touch is close to the bubble center (cancellation).
     * - Outer bounds: returns TARGET_NONE (-1) when touch is beyond maxSelectionRadius.
     * - Direct Euclidean Proximity: if the touch is within the node's hit circle, immediately selects it.
     * - Angular Sector Fallback with Schmitt-trigger / Hysteresis: applies angular tolerance cushions to
     *   prevent physiological tremor from jittering between adjacent targets.
     * - Isolated Edit sector with safety deadbands.
     */
    fun resolveTargetIndex(
        displacementX: Float,
        displacementY: Float,
        appCount: Int,
        isRightSide: Boolean,
        touchYRatio: Float, // 0.0f (top) to 1.0f (bottom)
        deadZoneRadius: Float = DEAD_ZONE_RADIUS_DP,
        editActivationRadius: Float = EDIT_ACTIVATION_RADIUS_DP,
        maxSelectionRadius: Float = MAX_SELECTION_RADIUS_DP,
        previousTargetIndex: Int = TARGET_NONE,
        arcRadiusPx: Float = 100f,
        nodeHitRadiusPx: Float = 32f
    ): Int {
        val radiusPx = hypot(displacementX, displacementY)
        if (radiusPx < deadZoneRadius || radiusPx > maxSelectionRadius) {
            return TARGET_NONE
        }

        // 1. Direct Euclidean hit testing for Edit Node
        val (editX, editY) = calculateEditNodeOffset(arcRadiusPx * 0.95f, isRightSide, touchYRatio)
        val editDist = hypot(displacementX - editX, displacementY - editY)
        var closestTarget = if (editDist <= nodeHitRadiusPx) EDIT_NODE_INDEX else TARGET_NONE
        var minDistance = if (editDist <= nodeHitRadiusPx) editDist else Float.MAX_VALUE

        // 2. Direct Euclidean hit testing for App Nodes (find the closest node)
        for (i in 0 until appCount) {
            val (nodeX, nodeY) = calculateNodeOffset(i, appCount, arcRadiusPx, isRightSide, touchYRatio)
            val dist = hypot(displacementX - nodeX, displacementY - nodeY)
            if (dist <= nodeHitRadiusPx && dist < minDistance) {
                minDistance = dist
                closestTarget = i
            }
        }

        if (closestTarget != TARGET_NONE) {
            return closestTarget
        }

        val inwardX = if (isRightSide) -displacementX else displacementX
        val rawAngleRad = atan2(displacementY, inwardX)
        val angleDeg = Math.toDegrees(rawAngleRad.toDouble()).toFloat()

        val baseStartAngle = calculateBaseStartAngle(touchYRatio)

        // 3. Edit Node Angular Detection (Separated by safety deadzone: baseStartAngle + 120°..155°)
        val editStartAngle = baseStartAngle + EDIT_START_OFFSET
        val editEndAngle = editStartAngle + EDIT_SWEEP
        val isPrevEdit = (previousTargetIndex == EDIT_NODE_INDEX)
        val editAngleTolerance = if (isPrevEdit) 8f else 0f
        val effectiveEditActivationRadius = if (isPrevEdit) editActivationRadius * 0.70f else editActivationRadius * 0.80f

        if (angleDeg in (editStartAngle - editAngleTolerance)..(editEndAngle + editAngleTolerance) &&
            radiusPx >= effectiveEditActivationRadius
        ) {
            return EDIT_NODE_INDEX
        }

        if (appCount <= 0) return TARGET_NONE

        // 4. App Sector Angular Detection with Hysteresis
        val sectorSize = TOTAL_APP_SWEEP / appCount
        val relativeAngle = angleDeg - baseStartAngle

        // Bounded sector sweep with hysteresis entry/exit margin
        val sweepTolerance = if (previousTargetIndex in 0 until appCount) 6f else 0f
        if (relativeAngle < -sweepTolerance || relativeAngle > TOTAL_APP_SWEEP + sweepTolerance) {
            return TARGET_NONE
        }

        val clampedRelAngle = relativeAngle.coerceIn(0f, TOTAL_APP_SWEEP - 0.001f)
        val rawSector = (clampedRelAngle / sectorSize).toInt().coerceIn(0, appCount - 1)

        // Apply hysteresis if user was already hovering an app node
        if (previousTargetIndex in 0 until appCount) {
            val prevSectorCenter = (previousTargetIndex + 0.5f) * sectorSize
            val angleDiffFromPrev = clampedRelAngle - prevSectorCenter
            val maxRetentionAngle = (sectorSize * 0.5f) * (1f + HYSTERESIS_SECTOR_RATIO)

            if (kotlin.math.abs(angleDiffFromPrev) <= maxRetentionAngle) {
                return previousTargetIndex
            }
        }

        return rawSector
    }

    /**
     * Calculates the Cartesian offset (x, y) for an app node in the radial fan.
     */
    fun calculateNodeOffset(
        index: Int,
        totalApps: Int,
        radiusPx: Float,
        isRightSide: Boolean,
        touchYRatio: Float
    ): Pair<Float, Float> {
        if (totalApps <= 0) return Pair(0f, 0f)
        val baseStartAngle = calculateBaseStartAngle(touchYRatio)
        val sectorSize = TOTAL_APP_SWEEP / totalApps
        val angleDeg = baseStartAngle + (index + 0.5f) * sectorSize
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val x = (radiusPx * cos(angleRad)).toFloat() * (if (isRightSide) -1f else 1f)
        val y = (radiusPx * sin(angleRad)).toFloat()
        return Pair(x, y)
    }

    /**
     * Calculates the Cartesian offset (x, y) for the outlying Edit Node.
     */
    fun calculateEditNodeOffset(
        radiusPx: Float,
        isRightSide: Boolean,
        touchYRatio: Float
    ): Pair<Float, Float> {
        val baseStartAngle = calculateBaseStartAngle(touchYRatio)
        val editAngleDeg = baseStartAngle + EDIT_START_OFFSET + EDIT_SWEEP / 2f
        val angleRad = Math.toRadians(editAngleDeg.toDouble())

        val x = (radiusPx * cos(angleRad)).toFloat() * (if (isRightSide) -1f else 1f)
        val y = (radiusPx * sin(angleRad)).toFloat()
        return Pair(x, y)
    }

    /**
     * Hit-tests direct taps/touches on individual nodes in Cartesian space relative to bubble center.
     * Returns: 0..N-1 for app nodes, -2 for Edit node, -1 for none.
     */
    fun findTappedNode(
        displacementX: Float,
        displacementY: Float,
        appCount: Int,
        isRightSide: Boolean,
        touchYRatio: Float,
        arcRadiusPx: Float,
        hitRadiusPx: Float
    ): Int {
        // 1. Check Edit Node
        val (editX, editY) = calculateEditNodeOffset(arcRadiusPx * 0.95f, isRightSide, touchYRatio)
        if (hypot(displacementX - editX, displacementY - editY) <= hitRadiusPx) {
            return EDIT_NODE_INDEX
        }

        // 2. Check App Nodes
        for (i in 0 until appCount) {
            val (nodeX, nodeY) = calculateNodeOffset(i, appCount, arcRadiusPx, isRightSide, touchYRatio)
            if (hypot(displacementX - nodeX, displacementY - nodeY) <= hitRadiusPx) {
                return i
            }
        }

        return TARGET_NONE
    }
}
