package com.velocity.launcher.ui.compose.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.velocity.launcher.data.BackgroundType
import com.velocity.launcher.data.ColorTone
import com.velocity.launcher.data.ScrollbarPosition
import com.velocity.launcher.data.ScrollbarVerticalAlignment
import com.velocity.launcher.data.SolidColorPreset
import com.velocity.launcher.data.ThemeMode
import com.velocity.launcher.data.TopSpacingMode
import com.velocity.launcher.data.WallpaperColorExtractor
import com.velocity.launcher.util.DefaultLauncherHelper
import kotlin.math.roundToInt

enum class SettingsPage {
    MAIN_HUB,
    LOOK_AND_FEEL,
    REACHABILITY,
    SCROLLBAR,
    APPS_SEARCH,
    SYSTEM
}

val SETTINGS_GLYPHS: List<String> = listOf("🎨", "📱", "🔤", "📦", "⚙️")

enum class SettingsFilterCategory(val label: String) {
    ALL("Semua"),
    LOOK_AND_FEEL("🎨 Tampilan"),
    REACHABILITY("📱 Ergonomi"),
    SCROLLBAR("🔤 Scrollbar"),
    APPS_SEARCH("📦 Aplikasi"),
    SYSTEM("⚙️ Sistem")
}

// Curated Accent Palette for Quick Selection
val CURATED_ACCENT_COLORS: List<Color> = listOf(
    Color(0xFF6366F1), // Royal Indigo
    Color(0xFF38BDF8), // Sky Blue
    Color(0xFF10B981), // Emerald Mint
    Color(0xFFF97316), // Sunset Coral
    Color(0xFFF43F5E), // Rose Pink
    Color(0xFFF59E0B), // Amber Gold
    Color(0xFF8B5CF6), // Violet Purple
    Color(0xFFEF4444), // Crimson Red
    Color(0xFF14B8A6), // Teal Cyan
    Color(0xFF94A3B8)  // Slate Silver
)

// Curated Wallpaper Filter Layer Colors
val CURATED_WALLPAPER_FILTER_COLORS: List<Color> = listOf(
    Color(0xFF000000), // Hitam
    Color(0xFF1E293B), // Charcoal
    Color(0xFF0F172A), // Midnight Navy
    Color(0xFF2D1B16), // Espresso
    Color(0xFFFFFFFF)  // Putih / Frost Light
)

// Curated Solid Background Colors for Custom Solid Mode
val CURATED_SOLID_BG_COLORS: List<Color> = listOf(
    Color(0xFF000000), // AMOLED Black
    Color(0xFF121212), // Deep Charcoal
    Color(0xFF0F172A), // Navy Slate
    Color(0xFF1E1E2E), // Midnight Slate
    Color(0xFF064E3B), // Forest Dark
    Color(0xFFF8F9FA), // Clean Light
    Color(0xFFFFFBEB), // Warm Cream
    Color(0xFFF1F5F9)  // Silver Light
)

@Composable
fun NirantaraSettingsScreen(
    scrollbarPosition: ScrollbarPosition,
    scrollbarVerticalAlignment: ScrollbarVerticalAlignment,
    themeMode: ThemeMode = ThemeMode.ADAPTIVE,
    backgroundType: BackgroundType = BackgroundType.WALLPAPER,
    colorTone: ColorTone = ColorTone.ADAPTIVE,
    topSpacingMode: TopSpacingMode,
    hapticFeedbackEnabled: Boolean,
    hiddenAppsCount: Int,
    topWidgetId: Int = -1,
    customWallpaperBitmap: ImageBitmap? = null,
    wallpaperColors: List<Color> = emptyList(),
    adaptiveAccentColor: Color? = null,
    solidColorPreset: SolidColorPreset = SolidColorPreset.AMOLED_BLACK,
    solidBackgroundColor: Color = Color(0xFF000000),
    solidAccentColor: Color = Color(0xFF818CF8),
    wallpaperFilterOpacity: Float = 0.40f,
    wallpaperFilterColor: Color = Color.Black,
    onScrollbarPositionChange: (ScrollbarPosition) -> Unit,
    onScrollbarVerticalAlignmentChange: (ScrollbarVerticalAlignment) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onBackgroundTypeChange: (BackgroundType) -> Unit = {},
    onColorToneChange: (ColorTone) -> Unit = {},
    onTopSpacingModeChange: (TopSpacingMode) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onOpenHiddenApps: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onAddTopWidgetClick: () -> Unit = {},
    onRemoveTopWidgetClick: () -> Unit = {},
    onPickWallpaperClick: () -> Unit = {},
    onRemoveWallpaperClick: () -> Unit = {},
    onAdaptiveAccentColorChange: (Color?) -> Unit = {},
    onWallpaperFilterOpacityChange: (Float) -> Unit = {},
    onWallpaperFilterColorChange: (Color) -> Unit = {},
    onSolidPresetChange: (SolidColorPreset) -> Unit = {},
    onSolidBackgroundColorChange: (Color) -> Unit = {},
    onSolidAccentColorChange: (Color) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN_HUB) }

    // Intercept Back Press: sub-page -> MAIN_HUB, MAIN_HUB -> onClose
    BackHandler {
        if (currentPage != SettingsPage.MAIN_HUB) {
            currentPage = SettingsPage.MAIN_HUB
        } else {
            onClose()
        }
    }

    val configuration = LocalConfiguration.current
    val topReachabilitySpacer = (configuration.screenHeightDp.dp * 0.12f).coerceIn(60.dp, 120.dp)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == SettingsPage.MAIN_HUB) {
                    (slideInHorizontally(animationSpec = tween(280)) { width -> -width / 3 } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> width } + fadeOut(animationSpec = tween(200)))
                } else {
                    (slideInHorizontally(animationSpec = tween(280)) { width -> width } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> -width / 3 } + fadeOut(animationSpec = tween(200)))
                }.using(
                    SizeTransform(clip = false)
                )
            },
            label = "SettingsPageTransition",
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                SettingsPage.MAIN_HUB -> {
                    MainHubPage(
                        backgroundType = backgroundType,
                        colorTone = colorTone,
                        solidColorPreset = solidColorPreset,
                        topSpacingMode = topSpacingMode,
                        scrollbarPosition = scrollbarPosition,
                        scrollbarVerticalAlignment = scrollbarVerticalAlignment,
                        hapticFeedbackEnabled = hapticFeedbackEnabled,
                        hiddenAppsCount = hiddenAppsCount,
                        topWidgetId = topWidgetId,
                        topReachabilitySpacer = topReachabilitySpacer,
                        onNavigate = { targetPage -> currentPage = targetPage },
                        onClose = onClose
                    )
                }
                SettingsPage.LOOK_AND_FEEL -> {
                    LookAndFeelSubPage(
                        backgroundType = backgroundType,
                        colorTone = colorTone,
                        customWallpaperBitmap = customWallpaperBitmap,
                        wallpaperColors = wallpaperColors,
                        adaptiveAccentColor = adaptiveAccentColor,
                        solidColorPreset = solidColorPreset,
                        solidBackgroundColor = solidBackgroundColor,
                        solidAccentColor = solidAccentColor,
                        wallpaperFilterOpacity = wallpaperFilterOpacity,
                        wallpaperFilterColor = wallpaperFilterColor,
                        topReachabilitySpacer = topReachabilitySpacer,
                        onBackgroundTypeChange = { newBgType ->
                            onBackgroundTypeChange(newBgType)
                            onThemeModeChange(
                                if (newBgType == BackgroundType.SOLID) ThemeMode.SOLID_COLOR
                                else when (colorTone) {
                                    ColorTone.ADAPTIVE -> ThemeMode.ADAPTIVE
                                    ColorTone.DARK -> ThemeMode.DARK
                                    ColorTone.LIGHT -> ThemeMode.LIGHT
                                    ColorTone.SYSTEM -> ThemeMode.SYSTEM
                                }
                            )
                        },
                        onColorToneChange = { newTone ->
                            onColorToneChange(newTone)
                            onThemeModeChange(
                                if (backgroundType == BackgroundType.SOLID) ThemeMode.SOLID_COLOR
                                else when (newTone) {
                                    ColorTone.ADAPTIVE -> ThemeMode.ADAPTIVE
                                    ColorTone.DARK -> ThemeMode.DARK
                                    ColorTone.LIGHT -> ThemeMode.LIGHT
                                    ColorTone.SYSTEM -> ThemeMode.SYSTEM
                                }
                            )
                        },
                        onPickWallpaperClick = onPickWallpaperClick,
                        onRemoveWallpaperClick = onRemoveWallpaperClick,
                        onAdaptiveAccentColorChange = onAdaptiveAccentColorChange,
                        onWallpaperFilterOpacityChange = onWallpaperFilterOpacityChange,
                        onWallpaperFilterColorChange = onWallpaperFilterColorChange,
                        onSolidPresetChange = onSolidPresetChange,
                        onSolidBackgroundColorChange = onSolidBackgroundColorChange,
                        onSolidAccentColorChange = onSolidAccentColorChange,
                        onBack = { currentPage = SettingsPage.MAIN_HUB }
                    )
                }
                SettingsPage.REACHABILITY -> {
                    ReachabilitySubPage(
                        topSpacingMode = topSpacingMode,
                        topReachabilitySpacer = topReachabilitySpacer,
                        onTopSpacingModeChange = onTopSpacingModeChange,
                        onBack = { currentPage = SettingsPage.MAIN_HUB }
                    )
                }
                SettingsPage.SCROLLBAR -> {
                    ScrollbarSubPage(
                        scrollbarPosition = scrollbarPosition,
                        scrollbarVerticalAlignment = scrollbarVerticalAlignment,
                        hapticFeedbackEnabled = hapticFeedbackEnabled,
                        topReachabilitySpacer = topReachabilitySpacer,
                        onScrollbarPositionChange = onScrollbarPositionChange,
                        onScrollbarVerticalAlignmentChange = onScrollbarVerticalAlignmentChange,
                        onHapticFeedbackChange = onHapticFeedbackChange,
                        onBack = { currentPage = SettingsPage.MAIN_HUB }
                    )
                }
                SettingsPage.APPS_SEARCH -> {
                    AppsSearchSubPage(
                        topWidgetId = topWidgetId,
                        hiddenAppsCount = hiddenAppsCount,
                        topReachabilitySpacer = topReachabilitySpacer,
                        onAddTopWidgetClick = onAddTopWidgetClick,
                        onRemoveTopWidgetClick = onRemoveTopWidgetClick,
                        onOpenHiddenApps = onOpenHiddenApps,
                        onClearSearchHistory = onClearSearchHistory,
                        onBack = { currentPage = SettingsPage.MAIN_HUB }
                    )
                }
                SettingsPage.SYSTEM -> {
                    SystemSubPage(
                        topReachabilitySpacer = topReachabilitySpacer,
                        onBack = { currentPage = SettingsPage.MAIN_HUB }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Main Hub Screen
// ---------------------------------------------------------------------------
@Composable
private fun MainHubPage(
    backgroundType: BackgroundType,
    colorTone: ColorTone,
    solidColorPreset: SolidColorPreset,
    topSpacingMode: TopSpacingMode,
    scrollbarPosition: ScrollbarPosition,
    scrollbarVerticalAlignment: ScrollbarVerticalAlignment,
    hapticFeedbackEnabled: Boolean,
    hiddenAppsCount: Int,
    topWidgetId: Int,
    topReachabilitySpacer: Dp,
    onNavigate: (SettingsPage) -> Unit,
    onClose: () -> Unit
) {
    val bgSummary = when (backgroundType) {
        BackgroundType.WALLPAPER -> "Wallpaper"
        BackgroundType.SOLID -> "Warna Solid (${solidColorPreset.title})"
    }
    val toneSummary = when (colorTone) {
        ColorTone.ADAPTIVE -> "Adaptif"
        ColorTone.DARK -> "Gelap"
        ColorTone.LIGHT -> "Terang"
        ColorTone.SYSTEM -> "Ikuti Sistem"
    }
    val themeSubtitle = "$bgSummary • $toneSummary"

    val reachabilitySubtitle = when (topSpacingMode) {
        TopSpacingMode.NORMAL -> "Normal (Offset 22%)"
        TopSpacingMode.LARGE -> "Besar (Offset 32%)"
        TopSpacingMode.COMPACT -> "Kompak (Offset 10%)"
        TopSpacingMode.NONE -> "Mati (Offset 0%)"
    }

    val posLabel = when (scrollbarPosition) {
        ScrollbarPosition.RIGHT -> "Kanan"
        ScrollbarPosition.LEFT -> "Kiri"
        ScrollbarPosition.BOTH -> "Keduanya"
    }
    val alignLabel = when (scrollbarVerticalAlignment) {
        ScrollbarVerticalAlignment.BOTTOM -> "Bawah"
        ScrollbarVerticalAlignment.CENTER -> "Tengah"
        ScrollbarVerticalAlignment.TOP -> "Atas"
    }
    val hapticLabel = if (hapticFeedbackEnabled) "Getar Aktif" else "Getar Nonaktif"
    val scrollbarSubtitle = "$posLabel • $alignLabel • $hapticLabel"

    val widgetSummary = if (topWidgetId != -1) "Widget header aktif" else "Atur widget header"
    val appsSubtitle = "$widgetSummary • $hiddenAppsCount aplikasi tersembunyi"
    val systemSubtitle = "Jadikan default • Versi 1.0"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Ergonomic Reachability Top Spacer & Header
            item(key = "hub_top_header") {
                Spacer(modifier = Modifier.height(topReachabilitySpacer))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tutup Pengaturan",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pengaturan Nirantara",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sesuaikan tampilan, ergonomi, dan navigasi",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 1. Tampilan (Look & Feel)
            item(key = "hub_look_and_feel") {
                HubCategoryRow(
                    icon = Icons.Default.Palette,
                    title = "Tampilan",
                    subtitle = themeSubtitle,
                    onClick = { onNavigate(SettingsPage.LOOK_AND_FEEL) }
                )
            }

            // 2. Ergonomi & Tata Letak (Reachability & Spacing)
            item(key = "hub_reachability") {
                HubCategoryRow(
                    icon = Icons.Default.TouchApp,
                    title = "Ergonomi & Tata Letak",
                    subtitle = reachabilitySubtitle,
                    onClick = { onNavigate(SettingsPage.REACHABILITY) }
                )
            }

            // 3. Scrollbar & Haptik (Alphabet Wave Indexer)
            item(key = "hub_scrollbar") {
                HubCategoryRow(
                    icon = Icons.Default.ViewHeadline,
                    title = "Scrollbar & Haptik",
                    subtitle = scrollbarSubtitle,
                    onClick = { onNavigate(SettingsPage.SCROLLBAR) }
                )
            }

            // 4. Aplikasi & Pencarian (Apps & Search)
            item(key = "hub_apps_search") {
                HubCategoryRow(
                    icon = Icons.Default.Apps,
                    title = "Aplikasi & Pencarian",
                    subtitle = appsSubtitle,
                    onClick = { onNavigate(SettingsPage.APPS_SEARCH) }
                )
            }

            // 5. Sistem & Peluncur Default (System)
            item(key = "hub_system") {
                HubCategoryRow(
                    icon = Icons.Default.Settings,
                    title = "Sistem & Peluncur Default",
                    subtitle = systemSubtitle,
                    onClick = { onNavigate(SettingsPage.SYSTEM) }
                )
            }
        }
    }
}

@Composable
private fun HubCategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    lineHeight = 16.sp
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// 2. Tampilan Sub-page (Look & Feel - Option 1: 2 Clean Sections)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookAndFeelSubPage(
    backgroundType: BackgroundType,
    colorTone: ColorTone,
    customWallpaperBitmap: ImageBitmap?,
    wallpaperColors: List<Color>,
    adaptiveAccentColor: Color?,
    solidColorPreset: SolidColorPreset,
    solidBackgroundColor: Color,
    solidAccentColor: Color,
    wallpaperFilterOpacity: Float = 0.40f,
    wallpaperFilterColor: Color = Color.Black,
    topReachabilitySpacer: Dp,
    onBackgroundTypeChange: (BackgroundType) -> Unit,
    onColorToneChange: (ColorTone) -> Unit,
    onPickWallpaperClick: () -> Unit,
    onRemoveWallpaperClick: () -> Unit,
    onAdaptiveAccentColorChange: (Color?) -> Unit,
    onWallpaperFilterOpacityChange: (Float) -> Unit = {},
    onWallpaperFilterColorChange: (Color) -> Unit = {},
    onSolidPresetChange: (SolidColorPreset) -> Unit,
    onSolidBackgroundColorChange: (Color) -> Unit,
    onSolidAccentColorChange: (Color) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // Top Header & Back Button
            item(key = "theme_top_header") {
                Spacer(modifier = Modifier.height(topReachabilitySpacer))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tampilan",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sesuaikan latar belakang dan nuansa warna tampilan peluncur",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // =========================================================================
            // SEKSI 1: LATAR BELAKANG (BACKGROUND)
            // =========================================================================
            item(key = "section_background_header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Latar Belakang",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f)
                        )
                    }
                    Text(
                        text = "Pilih antara gambar wallpaper atau warna solid murni",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
            }

            // Background Type Segmented Control
            item(key = "background_type_segmented_control") {
                FlatSegmentedControl(
                    items = listOf(BackgroundType.WALLPAPER, BackgroundType.SOLID),
                    selectedItem = backgroundType,
                    onItemSelected = onBackgroundTypeChange,
                    labelProvider = { type ->
                        when (type) {
                            BackgroundType.WALLPAPER -> "🖼️ Wallpaper"
                            BackgroundType.SOLID -> "⬛ Warna Solid"
                        }
                    }
                )
            }

            // Background Type Content Card
            item(key = "background_type_content") {
                AnimatedContent(
                    targetState = backgroundType,
                    label = "BackgroundTypeTransition"
                ) { currentBgType ->
                    when (currentBgType) {
                        BackgroundType.WALLPAPER -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Wallpaper Status / Thumbnail Preview
                                WallpaperPickerRow(
                                    customWallpaperBitmap = customWallpaperBitmap,
                                    onPickClick = onPickWallpaperClick,
                                    onRemoveClick = onRemoveWallpaperClick
                                )

                                // System Wallpaper Button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .clickable { WallpaperColorExtractor.openSystemWallpaperChooser(context) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = "Wallpaper Sistem HP",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Buka pemilih wallpaper bawaan perangkat Android",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Lapisan Filter Wallpaper
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Lapisan Filter Wallpaper",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                        )
                                        Text(
                                            text = "Atur opasitas dan warna lapisan filter untuk menyesuaikan dominasi wallpaper",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                            lineHeight = 16.sp
                                        )
                                    }

                                    // Opacity Row & Controls
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Opasitas Filter",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = "${(wallpaperFilterOpacity * 100).roundToInt()}%",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Slider(
                                            value = wallpaperFilterOpacity,
                                            onValueChange = onWallpaperFilterOpacityChange,
                                            valueRange = 0f..1f,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Preset Chips: 0%, 25%, 50%, 75%, 100%
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            listOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f).forEach { preset ->
                                                val presetPercent = (preset * 100).roundToInt()
                                                val currentPercent = (wallpaperFilterOpacity * 100).roundToInt()
                                                val isSelected = currentPercent == presetPercent

                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { onWallpaperFilterOpacityChange(preset) },
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                                ) {
                                                    Text(
                                                        text = "$presetPercent%",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Color Selector Row
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Warna Filter",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                        )

                                        val filterColorOptions = remember(wallpaperColors) {
                                            val list = CURATED_WALLPAPER_FILTER_COLORS.toMutableList()
                                            wallpaperColors.forEach { col ->
                                                if (col !in list) {
                                                    list.add(col)
                                                }
                                            }
                                            list
                                        }

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            items(filterColorOptions) { col ->
                                                val isSelected = wallpaperFilterColor == col
                                                ColorSwatchButton(
                                                    color = col,
                                                    isSelected = isSelected,
                                                    showBorder = true,
                                                    onClick = { onWallpaperFilterColorChange(col) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        BackgroundType.SOLID -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Pilihan Preset Warna Solid",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                                )

                                // Preset Buttons Grid / FlowRow
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SolidColorPreset.entries.forEach { preset ->
                                        val isSelected = solidColorPreset == preset
                                        val presetBg = when (preset) {
                                            SolidColorPreset.AMOLED_BLACK -> Color(0xFF000000)
                                            SolidColorPreset.DEEP_CHARCOAL -> Color(0xFF121212)
                                            SolidColorPreset.MIDNIGHT_SLATE -> Color(0xFF1E1E2E)
                                            SolidColorPreset.CLEAN_LIGHT -> Color(0xFFF8F9FA)
                                            SolidColorPreset.CUSTOM -> solidBackgroundColor
                                        }
                                        val isDark = presetBg.luminance() < 0.5f

                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onSolidPresetChange(preset) },
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(presetBg)
                                                        .border(
                                                            1.dp,
                                                            if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f),
                                                            CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = preset.title,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // If CUSTOM preset is selected, show background & accent swatches
                                AnimatedVisibility(
                                    visible = solidColorPreset == SolidColorPreset.CUSTOM,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Custom Background Swatches
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Warna Latar Belakang (Background)",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                                            )
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                items(CURATED_SOLID_BG_COLORS) { bgCol ->
                                                    val isSelected = solidBackgroundColor == bgCol
                                                    ColorSwatchButton(
                                                        color = bgCol,
                                                        isSelected = isSelected,
                                                        showBorder = true,
                                                        onClick = { onSolidBackgroundColorChange(bgCol) }
                                                    )
                                                }
                                            }
                                        }

                                        // Custom Accent Swatches
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Warna Aksen (Accent)",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                                            )
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                items(CURATED_ACCENT_COLORS) { accCol ->
                                                    val isSelected = solidAccentColor == accCol
                                                    ColorSwatchButton(
                                                        color = accCol,
                                                        isSelected = isSelected,
                                                        onClick = { onSolidAccentColorChange(accCol) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SEKSI 2: NUANSA WARNA (COLOR TONE)
            // =========================================================================
            item(key = "section_color_tone_header") {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Nuansa Warna",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f)
                        )
                    }
                    Text(
                        text = "Pilih gaya warna elemen antarmuka dan kontras tema",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
            }

            // Color Tone Segmented Control
            item(key = "color_tone_segmented_control") {
                FlatSegmentedControl(
                    items = listOf(ColorTone.ADAPTIVE, ColorTone.DARK, ColorTone.LIGHT, ColorTone.SYSTEM),
                    selectedItem = colorTone,
                    onItemSelected = onColorToneChange,
                    labelProvider = { tone ->
                        when (tone) {
                            ColorTone.ADAPTIVE -> "🎨 Adaptif"
                            ColorTone.DARK -> "🌙 Gelap"
                            ColorTone.LIGHT -> "☀️ Terang"
                            ColorTone.SYSTEM -> "⚙️ Sistem"
                        }
                    }
                )
            }

            // Color Tone Detail / Adaptive Swatches
            item(key = "color_tone_content") {
                when (colorTone) {
                    ColorTone.ADAPTIVE -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Wallpaper Palette Swatches (if wallpaper exists and extracted colors available)
                            if (wallpaperColors.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Palet Warna Wallpaper",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = "Warna aksen yang diekstrak langsung dari gambar wallpaper",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(wallpaperColors) { swatchColor ->
                                            val isSelected = adaptiveAccentColor == swatchColor
                                            ColorSwatchButton(
                                                color = swatchColor,
                                                isSelected = isSelected,
                                                onClick = { onAdaptiveAccentColorChange(swatchColor) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Curated Custom Accent Colors
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Warna Kustom",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "Pilih warna aksen kustom yang Anda sukai",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(CURATED_ACCENT_COLORS) { accentColor ->
                                        val isSelected = adaptiveAccentColor == accentColor
                                        ColorSwatchButton(
                                            color = accentColor,
                                            isSelected = isSelected,
                                            onClick = { onAdaptiveAccentColorChange(accentColor) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    ColorTone.DARK -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1E2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Mode Gelap Aktif",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Permukaan gelap, teks terang, dan scrim redup pada wallpaper untuk kenyamanan mata malam hari.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    ColorTone.LIGHT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = Color(0xFF333333),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Mode Terang Aktif",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Permukaan cerah, teks gelap, dan scrim terang pada wallpaper dengan kontras visual tinggi.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    ColorTone.SYSTEM -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrightnessAuto,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Mengikuti Sistem Android",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tema dan status bar otomatis berpindah antara mode terang dan gelap mengikuti jadwal perangkat Anda.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCardHeaderRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatchColor: Color? = null,
    swatchGradient: List<Color>? = null,
    isLightSwatch: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (swatchGradient != null) {
                            Modifier.background(Brush.linearGradient(swatchGradient))
                        } else if (swatchColor != null) {
                            Modifier.background(swatchColor)
                        } else {
                            Modifier.background(Color.Gray)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isLightSwatch) Color(0xFF333333) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    lineHeight = 16.sp
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WallpaperPickerRow(
    customWallpaperBitmap: ImageBitmap?,
    onPickClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (customWallpaperBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = customWallpaperBitmap,
                    contentDescription = "Preview Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wallpaper In-App Aktif",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Palet warna diekstrak otomatis",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Ganti", fontSize = 12.sp)
                    }

                    IconButton(onClick = onRemoveClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Wallpaper",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickClick)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pilih Wallpaper dari Galeri",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pilih gambar untuk latar belakang dan ekstraksi palet warna",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBorder: Boolean = false
) {
    val isLight = color.luminance() > 0.5f

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else if (showBorder) {
                    Modifier.border(1.dp, if (isLight) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.25f), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected Color",
                tint = if (isLight) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ThemeFlatSelectionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatchColor: Color? = null,
    swatchGradient: List<Color>? = null,
    isLightSwatch: Boolean = false
) {
    val rowBg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .then(
                        if (swatchGradient != null) {
                            Modifier.background(Brush.linearGradient(swatchGradient))
                        } else if (swatchColor != null) {
                            Modifier.background(swatchColor)
                        } else {
                            Modifier.background(Color.Gray)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isLightSwatch) Color(0xFF333333) else Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    lineHeight = 16.sp
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Ergonomi Sub-page (Reachability & Spacing)
// ---------------------------------------------------------------------------
@Composable
private fun ReachabilitySubPage(
    topSpacingMode: TopSpacingMode,
    topReachabilitySpacer: Dp,
    onTopSpacingModeChange: (TopSpacingMode) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "reachability_top_header") {
                Spacer(modifier = Modifier.height(topReachabilitySpacer))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ergonomi & Tata Letak",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sesuaikan posisi aplikasi agar selalu berada di zona nyaman satu tangan",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Normal Option
            item(key = "reachability_normal") {
                ReachabilityOptionRow(
                    title = "Normal",
                    tag = "Offset 22%",
                    subtitle = "Offset Seimbang. Aplikasi favorit dan daftar digeser ke bawah secara alami untuk kenyamanan jempol satu tangan.",
                    isSelected = topSpacingMode == TopSpacingMode.NORMAL,
                    onClick = { onTopSpacingModeChange(TopSpacingMode.NORMAL) }
                )
            }

            // Large Option
            item(key = "reachability_large") {
                ReachabilityOptionRow(
                    title = "Besar",
                    tag = "Offset 32%",
                    subtitle = "Offset Ekstra. Jangkauan bawah maksimal, sangat ideal untuk ponsel berlayar panjang atau penggunaan satu tangan intensif.",
                    isSelected = topSpacingMode == TopSpacingMode.LARGE,
                    onClick = { onTopSpacingModeChange(TopSpacingMode.LARGE) }
                )
            }

            // Compact Option
            item(key = "reachability_compact") {
                ReachabilityOptionRow(
                    title = "Kompak",
                    tag = "Offset 10%",
                    subtitle = "Offset Ringkas. Jarak atas minimal dengan ruang pandang lebih padat untuk menampilkan lebih banyak aplikasi sekaligus.",
                    isSelected = topSpacingMode == TopSpacingMode.COMPACT,
                    onClick = { onTopSpacingModeChange(TopSpacingMode.COMPACT) }
                )
            }

            // None Option
            item(key = "reachability_none") {
                ReachabilityOptionRow(
                    title = "Mati",
                    tag = "Offset 0%",
                    subtitle = "Tanpa Offset. Tata letak klasik sejajar di bagian paling atas layar tanpa jarak jangkauan ergonomis.",
                    isSelected = topSpacingMode == TopSpacingMode.NONE,
                    onClick = { onTopSpacingModeChange(TopSpacingMode.NONE) }
                )
            }
        }
    }
}

@Composable
private fun ReachabilityOptionRow(
    title: String,
    tag: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowBg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    lineHeight = 16.sp
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Scrollbar Sub-page (Alphabet Wave Indexer)
// ---------------------------------------------------------------------------
@Composable
private fun ScrollbarSubPage(
    scrollbarPosition: ScrollbarPosition,
    scrollbarVerticalAlignment: ScrollbarVerticalAlignment,
    hapticFeedbackEnabled: Boolean,
    topReachabilitySpacer: Dp,
    onScrollbarPositionChange: (ScrollbarPosition) -> Unit,
    onScrollbarVerticalAlignmentChange: (ScrollbarVerticalAlignment) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "scrollbar_top_header") {
                Spacer(modifier = Modifier.height(topReachabilitySpacer))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scrollbar & Haptik",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Konfigurasi posisi scrollbar alfabet dan getaran sentuhan haptik",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 1. Position Segmented Control
            item(key = "scrollbar_position_group") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Posisi Scrollbar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Pilih sisi layar untuk menempatkan indeks alfabet",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    FlatSegmentedControl(
                        items = listOf(
                            ScrollbarPosition.RIGHT,
                            ScrollbarPosition.LEFT,
                            ScrollbarPosition.BOTH
                        ),
                        selectedItem = scrollbarPosition,
                        onItemSelected = onScrollbarPositionChange,
                        labelProvider = { pos ->
                            when (pos) {
                                ScrollbarPosition.RIGHT -> "Kanan"
                                ScrollbarPosition.LEFT -> "Kiri"
                                ScrollbarPosition.BOTH -> "Keduanya"
                            }
                        }
                    )
                }
            }

            // 2. Alignment Segmented Control
            item(key = "scrollbar_alignment_group") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Perataan Vertikal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Posisi dasar alfabet saat tidak digeser",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    FlatSegmentedControl(
                        items = listOf(
                            ScrollbarVerticalAlignment.BOTTOM,
                            ScrollbarVerticalAlignment.CENTER,
                            ScrollbarVerticalAlignment.TOP
                        ),
                        selectedItem = scrollbarVerticalAlignment,
                        onItemSelected = onScrollbarVerticalAlignmentChange,
                        labelProvider = { align ->
                            when (align) {
                                ScrollbarVerticalAlignment.BOTTOM -> "Bawah"
                                ScrollbarVerticalAlignment.CENTER -> "Tengah"
                                ScrollbarVerticalAlignment.TOP -> "Atas"
                            }
                        }
                    )
                }
            }

            // 3. Haptic Feedback Switch Row
            item(key = "scrollbar_haptic_group") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                        .clickable { onHapticFeedbackChange(!hapticFeedbackEnabled) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hapticFeedbackEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = if (hapticFeedbackEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Getaran Haptik",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Getaran halus saat menggeser huruf indeks alfabet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                    }
                    Switch(
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = onHapticFeedbackChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            uncheckedTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. Aplikasi & Pencarian Sub-page (Apps & Search)
// ---------------------------------------------------------------------------
@Composable
private fun AppsSearchSubPage(
    topWidgetId: Int,
    hiddenAppsCount: Int,
    topReachabilitySpacer: Dp,
    onAddTopWidgetClick: () -> Unit,
    onRemoveTopWidgetClick: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "apps_search_top_header") {
                Spacer(modifier = Modifier.height(topReachabilitySpacer))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aplikasi & Pencarian",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kelola widget header, privasi aplikasi tersembunyi, dan data riwayat pencarian",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Widget Header Layar Utama Card
            item(key = "apps_header_widget_row") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (topWidgetId != -1) "Widget Header Aktif" else "Tambah Widget Header",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (topWidgetId != -1) "Widget disematkan di atas jam pada layar utama" else "Pilih widget Android untuk disematkan di atas jam",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }

                    if (topWidgetId != -1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ganti button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .clickable(onClick = onAddTopWidgetClick)
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Ganti",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Hapus button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                    .clickable {
                                        onRemoveTopWidgetClick()
                                        Toast.makeText(context, "Widget header telah dihapus", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Hapus",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    } else {
                        // Tambah Widget button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .clickable(onClick = onAddTopWidgetClick)
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Tambah Widget",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hidden Apps Row
            item(key = "apps_hidden_apps_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                        .clickable(onClick = onOpenHiddenApps)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Kelola Aplikasi Tersembunyi",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$hiddenAppsCount aplikasi disembunyikan dari drawer",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$hiddenAppsCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Clear Search History Row
            item(key = "apps_search_history_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Bersihkan Riwayat Pencarian",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hapus kata kunci pencarian yang tersimpan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable {
                                onClearSearchHistory()
                                Toast.makeText(context, "Riwayat pencarian telah dibersihkan", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Hapus",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. Sistem & Peluncur Default Sub-page (System)
// ---------------------------------------------------------------------------
@Composable
private fun SystemSubPage(
    topReachabilitySpacer: Dp,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDefault = remember { mutableStateOf(DefaultLauncherHelper.isDefaultLauncher(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefault.value = DefaultLauncherHelper.isDefaultLauncher(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "system_top_header") {
                Spacer(modifier = Modifier.height(topReachabilitySpacer))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sistem",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Atur Nirantara sebagai peluncur default dan lihat info aplikasi",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Set Default Launcher Button Row
            item(key = "system_default_launcher_row") {
                val isDefaultValue = isDefault.value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable {
                            DefaultLauncherHelper.openDefaultLauncherSettings(context)
                            isDefault.value = DefaultLauncherHelper.isDefaultLauncher(context)
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDefaultValue) Icons.Default.CheckCircle else Icons.Default.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isDefaultValue) "Peluncur Default Aktif" else "Jadikan Peluncur Default",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDefaultValue) {
                                    "Nirantara adalah home app default. Ketuk untuk mengelola di pengaturan sistem."
                                } else {
                                    "Buka pengaturan sistem Android untuk memilih home app"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // About Nirantara Card
            item(key = "system_about_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "N",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Nirantara",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Versi 1.0 • Minimal & Ergonomic",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Text(
                        text = "Didesain dengan Jetpack Compose untuk performa cepat, tampilan bersih tanpa kartu atau garis tepi, navigasi gelombang alfabet yang presisi, dan ergonomi jempol satu tangan.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Flat Segmented Control (Card-less & Border-less)
// ---------------------------------------------------------------------------
@Composable
private fun <T> FlatSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            } else {
                Color.Transparent
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(backgroundColor)
                    .clickable { onItemSelected(item) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(item),
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

