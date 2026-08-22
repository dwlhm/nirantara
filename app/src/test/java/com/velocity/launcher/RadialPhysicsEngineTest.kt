package com.velocity.launcher

import com.velocity.launcher.ui.compose.components.RadialPhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class RadialPhysicsEngineTest {

    private val delta = 0.001f

    @Test
    fun testBaseStartAngle_smoothInterpolation() {
        // Top edge: 0.0f -> 0.0°
        assertEquals(0f, RadialPhysicsEngine.calculateBaseStartAngle(0.0f), delta)
        assertEquals(-26f, RadialPhysicsEngine.calculateBaseStartAngle(0.25f), delta)

        // Center: 0.5f -> -52.0°
        assertEquals(-52f, RadialPhysicsEngine.calculateBaseStartAngle(0.50f), delta)

        // Bottom edge: 1.0f -> -80.0°
        assertEquals(-66f, RadialPhysicsEngine.calculateBaseStartAngle(0.75f), delta)
        assertEquals(-80f, RadialPhysicsEngine.calculateBaseStartAngle(1.0f), delta)

        // Clamping beyond 0.0f and 1.0f
        assertEquals(0f, RadialPhysicsEngine.calculateBaseStartAngle(-0.5f), delta)
        assertEquals(-80f, RadialPhysicsEngine.calculateBaseStartAngle(1.5f), delta)
    }

    @Test
    fun testResolveTargetIndex_withinDeadZone_returnsNegativeOne() {
        val result = RadialPhysicsEngine.resolveTargetIndex(
            displacementX = -10f,
            displacementY = 5f,
            appCount = 4,
            isRightSide = true,
            touchYRatio = 0.5f,
            deadZoneRadius = 28f
        )
        assertEquals(RadialPhysicsEngine.TARGET_NONE, result)
    }

    @Test
    fun testResolveTargetIndex_exceedsMaxSelectionRadius_returnsNegativeOne() {
        val result = RadialPhysicsEngine.resolveTargetIndex(
            displacementX = -200f,
            displacementY = 0f,
            appCount = 4,
            isRightSide = true,
            touchYRatio = 0.5f,
            deadZoneRadius = 28f,
            maxSelectionRadius = 140f
        )
        assertEquals(RadialPhysicsEngine.TARGET_NONE, result)
    }

    @Test
    fun testResolveTargetIndex_zeroAppCount_returnsNegativeOne() {
        val result = RadialPhysicsEngine.resolveTargetIndex(
            displacementX = -50f,
            displacementY = 0f,
            appCount = 0,
            isRightSide = true,
            touchYRatio = 0.5f
        )
        assertEquals(RadialPhysicsEngine.TARGET_NONE, result)
    }

    @Test
    fun testResolveTargetIndex_directEuclideanProximityHit() {
        val appCount = 4
        val arcRadius = 100f
        val touchYRatio = 0.5f

        for (i in 0 until appCount) {
            val (nodeX, nodeY) = RadialPhysicsEngine.calculateNodeOffset(i, appCount, arcRadius, isRightSide = true, touchYRatio = touchYRatio)
            // Exact touch on node
            val exactTarget = RadialPhysicsEngine.resolveTargetIndex(
                displacementX = nodeX,
                displacementY = nodeY,
                appCount = appCount,
                isRightSide = true,
                touchYRatio = touchYRatio,
                arcRadiusPx = arcRadius,
                nodeHitRadiusPx = 32f
            )
            assertEquals("Expected node $i for exact touch", i, exactTarget)

            // Touch with slight offset within hit radius (15px offset)
            val nearTarget = RadialPhysicsEngine.resolveTargetIndex(
                displacementX = nodeX + 10f,
                displacementY = nodeY - 10f,
                appCount = appCount,
                isRightSide = true,
                touchYRatio = touchYRatio,
                arcRadiusPx = arcRadius,
                nodeHitRadiusPx = 32f
            )
            assertEquals("Expected node $i for nearby proximity touch", i, nearTarget)
        }
    }

    @Test
    fun testResolveTargetIndex_appSectors_rightSide() {
        val appCount = 4
        val radius = 50f
        val touchYRatio = 0.5f // baseStartAngle = -52°
        val totalSweep = RadialPhysicsEngine.TOTAL_APP_SWEEP
        val sectorSize = totalSweep / appCount

        // For right side, inwardX = -dispX.
        for (i in 0 until appCount) {
            val angleDeg = -52f + (i + 0.5f) * sectorSize
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val dispX = -(radius * cos(angleRad)).toFloat()
            val dispY = (radius * sin(angleRad)).toFloat()

            val target = RadialPhysicsEngine.resolveTargetIndex(
                displacementX = dispX,
                displacementY = dispY,
                appCount = appCount,
                isRightSide = true,
                touchYRatio = touchYRatio
            )
            assertEquals("Expected sector $i for angle $angleDeg", i, target)
        }
    }

    @Test
    fun testResolveTargetIndex_appSectors_leftSide() {
        val appCount = 4
        val radius = 50f
        val touchYRatio = 0.5f // baseStartAngle = -52°
        val totalSweep = RadialPhysicsEngine.TOTAL_APP_SWEEP
        val sectorSize = totalSweep / appCount

        // For left side, inwardX = dispX
        for (i in 0 until appCount) {
            val angleDeg = -52f + (i + 0.5f) * sectorSize
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val dispX = (radius * cos(angleRad)).toFloat()
            val dispY = (radius * sin(angleRad)).toFloat()

            val target = RadialPhysicsEngine.resolveTargetIndex(
                displacementX = dispX,
                displacementY = dispY,
                appCount = appCount,
                isRightSide = false,
                touchYRatio = touchYRatio
            )
            assertEquals("Expected sector $i for left-side angle $angleDeg", i, target)
        }
    }

    @Test
    fun testResolveTargetIndex_magneticHysteresis_retainsPreviousTargetUnderJitter() {
        val appCount = 4
        val radius = 60f
        val touchYRatio = 0.5f // baseStartAngle = -52°
        val sectorSize = RadialPhysicsEngine.TOTAL_APP_SWEEP / appCount // 26°
        // Sector 0 is -52°..-26°. Boundary between sector 0 and 1 is -26°.
        // Test angle at -25° (just crossed into Sector 1 by 1 degree).
        val angleDeg = -25f
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val dispX = -(radius * cos(angleRad)).toFloat()
        val dispY = (radius * sin(angleRad)).toFloat()

        // Without hysteresis (previousTarget = TARGET_NONE), it resolves to Sector 1:
        val targetWithoutHysteresis = RadialPhysicsEngine.resolveTargetIndex(
            displacementX = dispX,
            displacementY = dispY,
            appCount = appCount,
            isRightSide = true,
            touchYRatio = touchYRatio,
            previousTargetIndex = RadialPhysicsEngine.TARGET_NONE
        )
        assertEquals(1, targetWithoutHysteresis)

        // WITH hysteresis (user was already hovering Sector 0), the 1-degree jitter is retained as Sector 0:
        val targetWithHysteresis = RadialPhysicsEngine.resolveTargetIndex(
            displacementX = dispX,
            displacementY = dispY,
            appCount = appCount,
            isRightSide = true,
            touchYRatio = touchYRatio,
            previousTargetIndex = 0
        )
        assertEquals(0, targetWithHysteresis)
    }

    @Test
    fun testResolveTargetIndex_editNode_detection() {
        val appCount = 4
        val radius = 60f
        val touchYRatio = 0.5f // baseStartAngle = -52°

        // Edit sector is baseStartAngle + 120°..155° (center = -52 + 137.5° = 85.5°)
        val editAngleDeg = -52f + 135f
        val angleRad = Math.toRadians(editAngleDeg.toDouble())
        val dispX = -(radius * cos(angleRad)).toFloat()
        val dispY = (radius * sin(angleRad)).toFloat()

        val target = RadialPhysicsEngine.resolveTargetIndex(
            displacementX = dispX,
            displacementY = dispY,
            appCount = appCount,
            isRightSide = true,
            touchYRatio = touchYRatio,
            editActivationRadius = 48f
        )
        assertEquals(RadialPhysicsEngine.EDIT_NODE_INDEX, target)
    }

    @Test
    fun testResolveTargetIndex_safetyDeadzoneBetweenAppsAndEdit_returnsNegativeOne() {
        val appCount = 4
        val radius = 60f
        val touchYRatio = 0.5f // baseStartAngle = -52°

        // Safety deadzone is between 116° and 118° relative (accounting for 12° sweep margin): angle = -52 + 117° = 65°
        val deadzoneAngleDeg = -52f + 117f
        val angleRad = Math.toRadians(deadzoneAngleDeg.toDouble())
        val dispX = -(radius * cos(angleRad)).toFloat()
        val dispY = (radius * sin(angleRad)).toFloat()

        val target = RadialPhysicsEngine.resolveTargetIndex(
            displacementX = dispX,
            displacementY = dispY,
            appCount = appCount,
            isRightSide = true,
            touchYRatio = touchYRatio
        )
        assertEquals(RadialPhysicsEngine.TARGET_NONE, target)
    }

    @Test
    fun testCalculateNodeOffset_symmetry() {
        val totalApps = 4
        val radiusPx = 80f

        val (rightX0, rightY0) = RadialPhysicsEngine.calculateNodeOffset(0, totalApps, radiusPx, isRightSide = true, touchYRatio = 0.5f)
        val (leftX0, leftY0) = RadialPhysicsEngine.calculateNodeOffset(0, totalApps, radiusPx, isRightSide = false, touchYRatio = 0.5f)

        // X offset on right is negative (inward to the left), on left is positive (inward to the right)
        assertEquals(-rightX0, leftX0, delta)
        assertEquals(rightY0, leftY0, delta)
        assertTrue(rightX0 < 0f)
    }

    @Test
    fun testCalculateEditNodeOffset_direction() {
        val radiusPx = 80f
        val (editRightX, editRightY) = RadialPhysicsEngine.calculateEditNodeOffset(radiusPx, isRightSide = true, touchYRatio = 0.5f)
        val (editLeftX, editLeftY) = RadialPhysicsEngine.calculateEditNodeOffset(radiusPx, isRightSide = false, touchYRatio = 0.5f)

        assertEquals(-editRightX, editLeftX, delta)
        assertEquals(editRightY, editLeftY, delta)
        assertTrue(editRightX < 0f)
        // Edit angle is tilted downwards (positive Y)
        assertTrue(editRightY > 0f)
    }

    @Test
    fun testFindTappedNode_directAppHit() {
        val appCount = 4
        val arcRadiusPx = 100f
        val hitRadiusPx = 32f

        // Get exact center of app node 2
        val (node2X, node2Y) = RadialPhysicsEngine.calculateNodeOffset(2, appCount, arcRadiusPx, isRightSide = true, touchYRatio = 0.5f)

        // Touch exactly on node 2
        val result = RadialPhysicsEngine.findTappedNode(
            displacementX = node2X,
            displacementY = node2Y,
            appCount = appCount,
            isRightSide = true,
            touchYRatio = 0.5f,
            arcRadiusPx = arcRadiusPx,
            hitRadiusPx = hitRadiusPx
        )
        assertEquals(2, result)
    }

    @Test
    fun testFindTappedNode_editNodeHit() {
        val appCount = 4
        val arcRadiusPx = 100f
        val hitRadiusPx = 32f

        // Get exact center of Edit node
        val (editX, editY) = RadialPhysicsEngine.calculateEditNodeOffset(arcRadiusPx * 0.95f, isRightSide = true, touchYRatio = 0.5f)

        val result = RadialPhysicsEngine.findTappedNode(
            displacementX = editX,
            displacementY = editY,
            appCount = appCount,
            isRightSide = true,
            touchYRatio = 0.5f,
            arcRadiusPx = arcRadiusPx,
            hitRadiusPx = hitRadiusPx
        )
        assertEquals(RadialPhysicsEngine.EDIT_NODE_INDEX, result)
    }

    @Test
    fun testFindTappedNode_missReturnsNegativeOne() {
        val appCount = 4
        val arcRadiusPx = 100f
        val hitRadiusPx = 32f

        // Touch far away
        val result = RadialPhysicsEngine.findTappedNode(
            displacementX = 0f,
            displacementY = 0f,
            appCount = appCount,
            isRightSide = true,
            touchYRatio = 0.5f,
            arcRadiusPx = arcRadiusPx,
            hitRadiusPx = hitRadiusPx
        )
        assertEquals(RadialPhysicsEngine.TARGET_NONE, result)
    }
}
