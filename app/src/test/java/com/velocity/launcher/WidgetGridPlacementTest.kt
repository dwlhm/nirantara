package com.velocity.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.SharedPreferences
import com.velocity.launcher.data.PreferencesManager
import com.velocity.launcher.data.WidgetGridPlacement
import com.velocity.launcher.ui.compose.components.GridWidgetItem
import com.velocity.launcher.ui.compose.components.WidgetGridLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetGridPlacementTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var preferencesManager: PreferencesManager

    private class FakeSharedPreferences : SharedPreferences {
        val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = EditorImpl()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class EditorImpl : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val removes = mutableSetOf<String>()
            private var clear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) temp[key] = values?.toSet()
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removes.add(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clear) data.clear()
                for (k in removes) data.remove(k)
                data.putAll(temp)
            }
        }
    }

    private class MockContext(private val prefs: SharedPreferences) : android.content.ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            return prefs
        }
    }

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        val mockContext = MockContext(fakePrefs)
        preferencesManager = PreferencesManager(mockContext)
    }

    // 1. Data class tests
    @Test
    fun testWidgetGridPlacement_defaultValues() {
        val placement = WidgetGridPlacement()
        assertEquals(0, placement.row)
        assertEquals(0, placement.startCol)
        assertEquals(8, placement.span)
    }

    @Test
    fun testWidgetGridPlacement_customValues() {
        val placement = WidgetGridPlacement(row = 2, startCol = 3, span = 4)
        assertEquals(2, placement.row)
        assertEquals(3, placement.startCol)
        assertEquals(4, placement.span)
    }

    @Test
    fun testWidgetGridPlacement_dataClassEquality() {
        val p1 = WidgetGridPlacement(row = 1, startCol = 2, span = 3)
        val p2 = WidgetGridPlacement(row = 1, startCol = 2, span = 3)
        val p3 = WidgetGridPlacement(row = 1, startCol = 2, span = 4)
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
        assertFalse(p1 == p3)
    }

    // 2. Serialization & Parsing tests
    @Test
    fun testPreferences_setAndGet3PartPlacement() {
        preferencesManager.setWidgetGridPlacement(widgetId = 101, row = 2, startCol = 3, span = 4)
        assertEquals("2_3_4", fakePrefs.getString("widget_grid_101", null))

        val retrieved = preferencesManager.getWidgetGridPlacement(101)
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.row)
        assertEquals(3, retrieved?.startCol)
        assertEquals(4, retrieved?.span)
    }

    @Test
    fun testPreferences_parse2PartLegacyFormat() {
        fakePrefs.edit().putString("widget_grid_102", "3_4").apply()

        val retrieved = preferencesManager.getWidgetGridPlacement(102)
        assertNotNull(retrieved)
        assertEquals(0, retrieved?.row)
        assertEquals(3, retrieved?.startCol)
        assertEquals(4, retrieved?.span)
    }

    @Test
    fun testPreferences_fallbackToLegacyCustomSpan() {
        preferencesManager.setWidgetCustomSpan(widgetId = 103, span = 3)
        val retrieved1 = preferencesManager.getWidgetGridPlacement(103)
        assertNotNull(retrieved1)
        assertEquals(0, retrieved1?.row)
        assertEquals(0, retrieved1?.startCol)
        assertEquals(6, retrieved1?.span) // 3 * 2 = 6

        preferencesManager.setWidgetCustomSpan(widgetId = 104, span = 7)
        val retrieved2 = preferencesManager.getWidgetGridPlacement(104)
        assertNotNull(retrieved2)
        assertEquals(0, retrieved2?.row)
        assertEquals(0, retrieved2?.startCol)
        assertEquals(7, retrieved2?.span)
    }

    @Test
    fun testPreferences_boundaryClamping() {
        // Negative row, negative startCol, excess span
        preferencesManager.setWidgetGridPlacement(widgetId = 105, row = -5, startCol = -2, span = 12)
        val retrieved = preferencesManager.getWidgetGridPlacement(105)
        assertNotNull(retrieved)
        assertEquals(0, retrieved?.row)
        assertEquals(0, retrieved?.startCol)
        assertEquals(8, retrieved?.span)

        // startCol = 6, span = 5 -> clamped span is 2 (6 + 2 = 8)
        preferencesManager.setWidgetGridPlacement(widgetId = 106, row = 3, startCol = 6, span = 5)
        val retrieved2 = preferencesManager.getWidgetGridPlacement(106)
        assertNotNull(retrieved2)
        assertEquals(3, retrieved2?.row)
        assertEquals(6, retrieved2?.startCol)
        assertEquals(2, retrieved2?.span)
    }

    @Test
    fun testPreferences_removePlacement() {
        preferencesManager.setWidgetGridPlacement(widgetId = 107, row = 1, startCol = 2, span = 3)
        assertNotNull(preferencesManager.getWidgetGridPlacement(107))
        preferencesManager.removeWidgetGridPlacement(107)
        assertNull(preferencesManager.getWidgetGridPlacement(107))
    }

    // 3. Collision Detection tests
    @Test
    fun testCollisionDetection_overlappingRanges() {
        // Partial overlap: [0..3] vs [2..5]
        assertTrue(WidgetGridLayoutEngine.isOverlapping(0, 4, 2, 4))
        assertTrue(WidgetGridLayoutEngine.isOverlapping(2, 4, 0, 4))

        // Subset overlap: [1..6] contains [2..4]
        assertTrue(WidgetGridLayoutEngine.isOverlapping(1, 6, 2, 3))
        assertTrue(WidgetGridLayoutEngine.isOverlapping(2, 3, 1, 6))

        // Single column boundary overlap: [0..2] and [2..4] share col 2
        assertTrue(WidgetGridLayoutEngine.isOverlapping(0, 3, 2, 3))

        // Identical ranges
        assertTrue(WidgetGridLayoutEngine.isOverlapping(3, 4, 3, 4))
    }

    @Test
    fun testCollisionDetection_adjacentNonOverlapping() {
        // [0..3] and [4..7] are adjacent, no overlap
        assertFalse(WidgetGridLayoutEngine.isOverlapping(0, 4, 4, 4))
        assertFalse(WidgetGridLayoutEngine.isOverlapping(4, 4, 0, 4))

        // [0..1] and [2..5]
        assertFalse(WidgetGridLayoutEngine.isOverlapping(0, 2, 2, 4))
        assertFalse(WidgetGridLayoutEngine.isOverlapping(2, 4, 0, 2))
    }

    @Test
    fun testCollisionDetection_disjointRanges() {
        // [0..1] and [6..7]
        assertFalse(WidgetGridLayoutEngine.isOverlapping(0, 2, 6, 2))
        assertFalse(WidgetGridLayoutEngine.isOverlapping(6, 2, 0, 2))
    }

    // 4. Slot Finding tests
    @Test
    fun testSlotFinding_emptyRow_returnsPreferred() {
        val slot = WidgetGridLayoutEngine.findFirstAvailableSlot(
            span = 4,
            existingItems = emptyList(),
            preferredStartCol = 3
        )
        assertEquals(3, slot)
    }

    @Test
    fun testSlotFinding_preferredCollides_findsFirstAvailableAlternative() {
        val dummyInfo = AppWidgetProviderInfo()
        val existing = listOf(
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 4)
        )
        // Preferred 0 collides with existing [0..3], first free slot of span 4 is 4
        val slot = WidgetGridLayoutEngine.findFirstAvailableSlot(
            span = 4,
            existingItems = existing,
            preferredStartCol = 0
        )
        assertEquals(4, slot)
    }

    @Test
    fun testSlotFinding_gapBetweenWidgets() {
        val dummyInfo = AppWidgetProviderInfo()
        val existing = listOf(
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 2),
            GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 6, span = 2)
        )
        // Free range is [2..5] (size 4)
        val slot = WidgetGridLayoutEngine.findFirstAvailableSlot(
            span = 4,
            existingItems = existing,
            preferredStartCol = 0
        )
        assertEquals(2, slot)
    }

    @Test
    fun testSlotFinding_noSlotAvailable_returnsNull() {
        val dummyInfo = AppWidgetProviderInfo()
        val existing = listOf(
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 8)
        )
        val slot = WidgetGridLayoutEngine.findFirstAvailableSlot(
            span = 2,
            existingItems = existing,
            preferredStartCol = 0
        )
        assertNull(slot)
    }

    // 5. Row Packing & Multi-Row Normalization tests
    @Test
    fun testPackWidgets_emptyList() {
        val packed = WidgetGridLayoutEngine.packWidgetsIntoRows(emptyList())
        assertTrue(packed.isEmpty())
    }

    @Test
    fun testPackWidgets_twoItemsSameRow_fitSideBySide() {
        val dummyInfo = AppWidgetProviderInfo()
        val items = listOf(
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 4),
            GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 4, span = 4)
        )
        val packed = WidgetGridLayoutEngine.packWidgetsIntoRows(items)
        assertEquals(1, packed.size)
        assertEquals(2, packed[0].size)
        assertEquals(0, packed[0][0].startCol)
        assertEquals(4, packed[0][0].span)
        assertEquals(4, packed[0][1].startCol)
        assertEquals(4, packed[0][1].span)
    }

    @Test
    fun testPackWidgets_twoItemsSameRow_colliding_overflowsToNextRow() {
        val dummyInfo = AppWidgetProviderInfo()
        val items = listOf(
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 8),
            GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 0, span = 8)
        )
        val packed = WidgetGridLayoutEngine.packWidgetsIntoRows(items)
        assertEquals(2, packed.size)
        assertEquals(1, packed[0].size)
        assertEquals(0, packed[0][0].row)
        assertEquals(1, packed[1].size)
        assertEquals(1, packed[1][0].row)
    }

    @Test
    fun testPackWidgets_multiRowNormalization() {
        val dummyInfo = AppWidgetProviderInfo()
        // Sparse rows (row 3 and row 7) normalized to row 0 and row 1
        val items = listOf(
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 3, startCol = 2, span = 4),
            GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 7, startCol = 0, span = 8)
        )
        val packed = WidgetGridLayoutEngine.packWidgetsIntoRows(items)
        assertEquals(2, packed.size)
        assertEquals(0, packed[0][0].row)
        assertEquals(2, packed[0][0].startCol)
        assertEquals(1, packed[1][0].row)
        assertEquals(0, packed[1][0].startCol)
    }

    @Test
    fun testPackWidgets_complexThreeRows() {
        val dummyInfo = AppWidgetProviderInfo()
        val items = listOf(
            // Row 0 candidates
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 4),
            GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 4, span = 4),
            GridWidgetItem(widgetId = 3, providerInfo = dummyInfo, row = 0, startCol = 0, span = 6), // overflows to row 1
            // Row 1 candidates
            GridWidgetItem(widgetId = 4, providerInfo = dummyInfo, row = 1, startCol = 6, span = 2), // fits with widget 3 on row 1
            GridWidgetItem(widgetId = 5, providerInfo = dummyInfo, row = 1, startCol = 0, span = 8)  // overflows to row 2
        )
        val packed = WidgetGridLayoutEngine.packWidgetsIntoRows(items)
        assertEquals(3, packed.size)

        // Row 0: widget 1 (0..3) and widget 2 (4..7)
        assertEquals(2, packed[0].size)
        assertEquals(1, packed[0][0].widgetId)
        assertEquals(2, packed[0][1].widgetId)

        // Row 1: widget 3 (0..5) and widget 4 (6..7)
        assertEquals(2, packed[1].size)
        assertEquals(3, packed[1][0].widgetId)
        assertEquals(4, packed[1][1].widgetId)

        // Row 2: widget 5 (0..7)
        assertEquals(1, packed[2].size)
        assertEquals(5, packed[2][0].widgetId)
        assertEquals(2, packed[2][0].row)
    }

    // 6. Left & Right Resize and Non-Overlapping Invariant tests
    @Test
    fun testResize_leftEdgeExpansionAndContraction() {
        val itemStartCol = 2
        val itemSpan = 4
        val currentEnd = itemStartCol + itemSpan - 1 // 5

        // Expand left: proposed start = 1, proposed span = 5 (cols 1..5)
        val proposedStartExpand = itemStartCol - 1
        val proposedSpanExpand = currentEnd - proposedStartExpand + 1
        assertEquals(1, proposedStartExpand)
        assertEquals(5, proposedSpanExpand)
        assertTrue(proposedStartExpand >= 0)
        assertTrue(proposedStartExpand + proposedSpanExpand <= 8)

        // Contract from left: proposed start = 3, proposed span = 3 (cols 3..5)
        val proposedStartContract = itemStartCol + 1
        val proposedSpanContract = currentEnd - proposedStartContract + 1
        assertEquals(3, proposedStartContract)
        assertEquals(3, proposedSpanContract)
        assertTrue(proposedSpanContract >= 1)
    }

    @Test
    fun testResize_rightEdgeExpansionAndContraction() {
        val itemStartCol = 3
        val itemSpan = 3 // cols 3..5

        // Expand right: proposed span = 4 (cols 3..6)
        val proposedSpanExpand = itemSpan + 1
        assertEquals(4, proposedSpanExpand)
        assertTrue(itemStartCol + proposedSpanExpand <= 8)

        // Contract from right: proposed span = 2 (cols 3..4)
        val proposedSpanContract = itemSpan - 1
        assertEquals(2, proposedSpanContract)
        assertTrue(proposedSpanContract >= 1)
    }

    @Test
    fun testResize_collisionWithNeighborPrevented() {
        val dummyInfo = AppWidgetProviderInfo()
        val widget1 = GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 4) // 0..3
        val widget2 = GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 4, span = 4) // 4..7
        val row = listOf(widget1, widget2)

        // Widget 2 trying to expand left (proposed start 3, proposed span 5 => 3..7)
        val proposedStart2 = 3
        val proposedSpan2 = 5
        val hasCollision2 = row.any { other ->
            other.widgetId != widget2.widgetId &&
            WidgetGridLayoutEngine.isOverlapping(proposedStart2, proposedSpan2, other.startCol, other.span)
        }
        assertTrue("Widget 2 expanding left must collide with Widget 1", hasCollision2)

        // Widget 1 trying to expand right (proposed start 0, proposed span 5 => 0..4)
        val proposedStart1 = 0
        val proposedSpan1 = 5
        val hasCollision1 = row.any { other ->
            other.widgetId != widget1.widgetId &&
            WidgetGridLayoutEngine.isOverlapping(proposedStart1, proposedSpan1, other.startCol, other.span)
        }
        assertTrue("Widget 1 expanding right must collide with Widget 2", hasCollision1)
    }

    // 7. Dense Surroundings & Real-World Layout Scenarios
    @Test
    fun testSurroundedWidget_resizeLeft_stopsAtLeftNeighbor() {
        val dummyInfo = AppWidgetProviderInfo()
        val leftNeighbor = GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 2) // [0..1]
        val centerWidget = GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 2, span = 4) // [2..5]
        val rightNeighbor = GridWidgetItem(widgetId = 3, providerInfo = dummyInfo, row = 0, startCol = 6, span = 2) // [6..7]
        val row = listOf(leftNeighbor, centerWidget, rightNeighbor)

        // Center widget expanding left: startCol 2 -> 1, span 4 -> 5 (cols [1..5])
        val proposedStart = centerWidget.startCol - 1
        val currentEnd = centerWidget.startCol + centerWidget.span - 1
        val proposedSpan = currentEnd - proposedStart + 1

        val hasCollision = row.any { other ->
            other.widgetId != centerWidget.widgetId &&
            WidgetGridLayoutEngine.isOverlapping(proposedStart, proposedSpan, other.startCol, other.span)
        }

        assertTrue("Center widget expanding left to col 1 must collide with left neighbor at [0..1]", hasCollision)
    }

    @Test
    fun testSurroundedWidget_resizeRight_stopsAtRightNeighbor() {
        val dummyInfo = AppWidgetProviderInfo()
        val leftNeighbor = GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 2) // [0..1]
        val centerWidget = GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 2, span = 4) // [2..5]
        val rightNeighbor = GridWidgetItem(widgetId = 3, providerInfo = dummyInfo, row = 0, startCol = 6, span = 2) // [6..7]
        val row = listOf(leftNeighbor, centerWidget, rightNeighbor)

        // Center widget expanding right: span 4 -> 5 (cols [2..6])
        val proposedSpan = centerWidget.span + 1

        val hasCollision = row.any { other ->
            other.widgetId != centerWidget.widgetId &&
            WidgetGridLayoutEngine.isOverlapping(centerWidget.startCol, proposedSpan, other.startCol, other.span)
        }

        assertTrue("Center widget expanding right to col 6 must collide with right neighbor at [6..7]", hasCollision)
    }

    @Test
    fun testSurroundedWidget_contractFromBothSides_succeeds() {
        val dummyInfo = AppWidgetProviderInfo()
        val leftNeighbor = GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 2) // [0..1]
        val centerWidget = GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 2, span = 4) // [2..5]
        val rightNeighbor = GridWidgetItem(widgetId = 3, providerInfo = dummyInfo, row = 0, startCol = 6, span = 2) // [6..7]
        val row = listOf(leftNeighbor, centerWidget, rightNeighbor)
        val currentEnd = centerWidget.startCol + centerWidget.span - 1

        // 1. Contract from left: startCol 2 -> 3, span 4 -> 3 (cols [3..5])
        val proposedStartLeft = centerWidget.startCol + 1
        val proposedSpanLeft = currentEnd - proposedStartLeft + 1
        assertEquals(3, proposedStartLeft)
        assertEquals(3, proposedSpanLeft)

        val collisionContractLeft = row.any { other ->
            other.widgetId != centerWidget.widgetId &&
            WidgetGridLayoutEngine.isOverlapping(proposedStartLeft, proposedSpanLeft, other.startCol, other.span)
        }
        assertFalse("Contracting from left must not collide with any neighbor", collisionContractLeft)
        assertTrue("Contracted startCol must be within valid range [0..7]", proposedStartLeft >= 0)
        assertTrue("Contracted span must be at least 1 and within row bounds", proposedSpanLeft >= 1 && proposedStartLeft + proposedSpanLeft <= 8)

        // 2. Contract from right: startCol 2, span 4 -> 3 (cols [2..4])
        val proposedSpanRight = centerWidget.span - 1
        assertEquals(3, proposedSpanRight)

        val collisionContractRight = row.any { other ->
            other.widgetId != centerWidget.widgetId &&
            WidgetGridLayoutEngine.isOverlapping(centerWidget.startCol, proposedSpanRight, other.startCol, other.span)
        }
        assertFalse("Contracting from right must not collide with any neighbor", collisionContractRight)
        assertTrue("Contracted span must be at least 1 and within row bounds", proposedSpanRight >= 1 && centerWidget.startCol + proposedSpanRight <= 8)
    }

    @Test
    fun testDenseThreeRowLayout_packingAndInvariants() {
        val dummyInfo = AppWidgetProviderInfo()
        val items = listOf(
            // Row 0: 4 + 4
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 4),
            GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 4, span = 4),
            // Row 1: 3 + 2 + 3
            GridWidgetItem(widgetId = 3, providerInfo = dummyInfo, row = 1, startCol = 0, span = 3),
            GridWidgetItem(widgetId = 4, providerInfo = dummyInfo, row = 1, startCol = 3, span = 2),
            GridWidgetItem(widgetId = 5, providerInfo = dummyInfo, row = 1, startCol = 5, span = 3),
            // Row 2: 8
            GridWidgetItem(widgetId = 6, providerInfo = dummyInfo, row = 2, startCol = 0, span = 8)
        )

        val packed = WidgetGridLayoutEngine.packWidgetsIntoRows(items)
        assertEquals(3, packed.size)

        // Validate Row 0
        assertEquals(2, packed[0].size)
        assertEquals(0, packed[0][0].row)
        assertEquals(0, packed[0][0].startCol)
        assertEquals(4, packed[0][0].span)
        assertEquals(0, packed[0][1].row)
        assertEquals(4, packed[0][1].startCol)
        assertEquals(4, packed[0][1].span)

        // Validate Row 1
        assertEquals(3, packed[1].size)
        assertEquals(1, packed[1][0].row)
        assertEquals(0, packed[1][0].startCol)
        assertEquals(3, packed[1][0].span)
        assertEquals(1, packed[1][1].row)
        assertEquals(3, packed[1][1].startCol)
        assertEquals(2, packed[1][1].span)
        assertEquals(1, packed[1][2].row)
        assertEquals(5, packed[1][2].startCol)
        assertEquals(3, packed[1][2].span)

        // Validate Row 2
        assertEquals(1, packed[2].size)
        assertEquals(2, packed[2][0].row)
        assertEquals(0, packed[2][0].startCol)
        assertEquals(8, packed[2][0].span)

        // Validate non-overlapping invariant for every row
        for (row in packed) {
            for (i in row.indices) {
                for (j in (i + 1) until row.size) {
                    val itemA = row[i]
                    val itemB = row[j]
                    assertFalse(
                        "Items ${itemA.widgetId} and ${itemB.widgetId} on row ${itemA.row} must not overlap",
                        WidgetGridLayoutEngine.isOverlapping(itemA.startCol, itemA.span, itemB.startCol, itemB.span)
                    )
                }
            }
        }
    }

    @Test
    fun testTightGap_widgetPlacementAndResizeConstraints() {
        val dummyInfo = AppWidgetProviderInfo()
        val leftNeighbor = GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 3) // [0..2]
        val rightNeighbor = GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 5, span = 3) // [5..7]
        val existing = listOf(leftNeighbor, rightNeighbor)

        // Gap is [3..4] (span 2)
        val preferredStart = 3
        val span = 2
        val slot = WidgetGridLayoutEngine.findFirstAvailableSlot(
            span = span,
            existingItems = existing,
            preferredStartCol = preferredStart
        )
        assertEquals(3, slot)

        val middleWidget = GridWidgetItem(widgetId = 3, providerInfo = dummyInfo, row = 0, startCol = slot!!, span = span)
        val fullRow = existing + middleWidget

        // Middle widget expands left: proposedStart 2, proposedSpan 3 => [2..4]
        val expandLeftStart = middleWidget.startCol - 1
        val currentEnd = middleWidget.startCol + middleWidget.span - 1
        val expandLeftSpan = currentEnd - expandLeftStart + 1
        val collideLeft = fullRow.any { other ->
            other.widgetId != middleWidget.widgetId &&
            WidgetGridLayoutEngine.isOverlapping(expandLeftStart, expandLeftSpan, other.startCol, other.span)
        }
        assertTrue("Expanding left across tight gap must collide with left neighbor", collideLeft)

        // Middle widget expands right: proposedStart 3, proposedSpan 3 => [3..5]
        val expandRightSpan = middleWidget.span + 1
        val collideRight = fullRow.any { other ->
            other.widgetId != middleWidget.widgetId &&
            WidgetGridLayoutEngine.isOverlapping(middleWidget.startCol, expandRightSpan, other.startCol, other.span)
        }
        assertTrue("Expanding right across tight gap must collide with right neighbor", collideRight)
    }

    @Test
    fun testMultiRowCascadeOverflow_withDenseSurroundings() {
        val dummyInfo = AppWidgetProviderInfo()
        val items = listOf(
            // Row 0 candidates: total span requested = 4 + 4 + 4 = 12 (widget 3 overflows to row 1)
            GridWidgetItem(widgetId = 1, providerInfo = dummyInfo, row = 0, startCol = 0, span = 4),
            GridWidgetItem(widgetId = 2, providerInfo = dummyInfo, row = 0, startCol = 4, span = 4),
            GridWidgetItem(widgetId = 3, providerInfo = dummyInfo, row = 0, startCol = 0, span = 4),
            // Row 1 candidates: widget 4 (span 3), widget 5 (span 3)
            GridWidgetItem(widgetId = 4, providerInfo = dummyInfo, row = 1, startCol = 0, span = 3),
            GridWidgetItem(widgetId = 5, providerInfo = dummyInfo, row = 1, startCol = 3, span = 3)
        )

        val packed = WidgetGridLayoutEngine.packWidgetsIntoRows(items)
        assertEquals(3, packed.size)

        // Row 0: widget 1 and widget 2
        assertEquals(2, packed[0].size)
        assertEquals(listOf(1, 2), packed[0].map { it.widgetId })

        // Row 1: widget 3 (overflowed from row 0, placed at 0..3) and widget 4 (placed at 4..6)
        assertEquals(2, packed[1].size)
        assertEquals(listOf(3, 4), packed[1].map { it.widgetId })
        assertEquals(1, packed[1][0].row)
        assertEquals(0, packed[1][0].startCol)
        assertEquals(4, packed[1][0].span)
        assertEquals(1, packed[1][1].row)
        assertEquals(4, packed[1][1].startCol)
        assertEquals(3, packed[1][1].span)

        // Row 2: widget 5 (cascaded from row 1 because only 1 column [col 7] was left on row 1)
        assertEquals(1, packed[2].size)
        assertEquals(5, packed[2][0].widgetId)
        assertEquals(2, packed[2][0].row)
        assertEquals(3, packed[2][0].startCol)
        assertEquals(3, packed[2][0].span)
    }

    @Test
    fun testBoundaryResizing_atScreenEdges() {
        // Left boundary: widget at startCol 0
        val startColLeft = 0
        val spanLeft = 4
        val canExpandLeft = startColLeft > 0
        assertFalse("Widget at left screen boundary (startCol=0) cannot expand left", canExpandLeft)

        // Right boundary: widget at startCol 4, span 4 (end = 7)
        val startColRight = 4
        val spanRight = 4
        val proposedSpanExpandRight = spanRight + 1
        val canExpandRight = (startColRight + proposedSpanExpandRight) <= 8
        assertFalse("Widget at right screen boundary (startCol+span=8) cannot expand right", canExpandRight)

        // Full width boundary: widget at startCol 0, span 8
        val startColFull = 0
        val spanFull = 8
        val canExpandFullLeft = startColFull > 0
        val canExpandFullRight = (startColFull + spanFull + 1) <= 8
        assertFalse("Full-width widget cannot expand left", canExpandFullLeft)
        assertFalse("Full-width widget cannot expand right", canExpandFullRight)
    }
}
