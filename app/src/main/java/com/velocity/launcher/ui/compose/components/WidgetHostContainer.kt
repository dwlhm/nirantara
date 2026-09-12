package com.velocity.launcher.ui.compose.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.velocity.launcher.ui.widget.ScrollAwareAppWidgetHostView
import com.velocity.launcher.ui.widget.WidgetSnapshotManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val MIN_WIDGET_FALLBACK_HEIGHT_DP = 80
private const val LIVE_LOAD_DELAY_MS = 300L

@Composable
fun WidgetHostContainer(
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    span: Int = 8,
    customHeightDp: Int? = null,
    isEditing: Boolean = false,
    isScrollInProgress: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (appWidgetId == -1) return

    val appWidgetInfo = remember(appWidgetId) {
        appWidgetManager.getAppWidgetInfo(appWidgetId)
    }

    if (appWidgetInfo == null) return

    val context = LocalContext.current
    val resolvedHeightDp = customHeightDp?.takeIf { it > 0 } ?: appWidgetInfo.minHeight.coerceAtLeast(MIN_WIDGET_FALLBACK_HEIGHT_DP)
    val heightModifier = Modifier.height(resolvedHeightDp.dp)

    var snapshotBitmap by remember(appWidgetId) {
        mutableStateOf<Bitmap?>(null)
    }

    var hasSnapshotSaved by remember(appWidgetId) { mutableStateOf(false) }

    LaunchedEffect(appWidgetId) {
        snapshotBitmap = withContext(Dispatchers.IO) {
            WidgetSnapshotManager.getSnapshot(context, appWidgetId)
        }
    }

    var isLiveActive by remember(appWidgetId) { mutableStateOf(isEditing) }

    LaunchedEffect(appWidgetId, isEditing, isScrollInProgress) {
        if (isEditing) {
            isLiveActive = true
        } else {
            // Small delay so initial scroll / cold start render is instant and 120 FPS pure canvas
            if (isScrollInProgress) return@LaunchedEffect
            delay(LIVE_LOAD_DELAY_MS)
            isLiveActive = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier),
        contentAlignment = Alignment.Center
    ) {
        if (!isLiveActive) {
            val snapshot = snapshotBitmap
            if (snapshot != null) {
                Image(
                    bitmap = snapshot.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(heightModifier),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(heightModifier)
                )
            }
        } else {
            key(appWidgetId) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(heightModifier),
                    factory = { factoryContext: Context ->
                        try {
                            val hostView = appWidgetHost.createView(factoryContext, appWidgetId, appWidgetInfo)
                            if (hostView is ScrollAwareAppWidgetHostView) {
                                hostView.onLongPressListener = onLongPress
                            }
                            hostView.layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            hostView.setAppWidget(appWidgetId, appWidgetInfo)
                            hostView.setPadding(0, 0, 0, 0)
                            
                            val density = factoryContext.resources.displayMetrics.density
                            val screenWidthPx = factoryContext.resources.displayMetrics.widthPixels
                            val totalWidthDp = (screenWidthPx / density).coerceAtLeast(100f)
                            val widthDp = ((totalWidthDp * (span / 8.0f)).toInt()).coerceAtLeast(80)
                            val heightDp = resolvedHeightDp
                            val options = android.os.Bundle().apply {
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                            }
                            try {
                                hostView.updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp)
                                appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
                            } catch (e: Exception) {}

                            hostView.tag = (widthDp to heightDp)
                            hostView
                        } catch (e: Exception) {
                            AppWidgetHostView(factoryContext)
                        }
                    },
                    update = { hostView ->
                        try {
                            if (hostView is ScrollAwareAppWidgetHostView) {
                                hostView.onLongPressListener = onLongPress
                            }
                            val density = hostView.context.resources.displayMetrics.density
                            val screenWidthPx = hostView.context.resources.displayMetrics.widthPixels
                            val totalWidthDp = (screenWidthPx / density).coerceAtLeast(100f)
                            val widthDp = ((totalWidthDp * (span / 8.0f)).toInt()).coerceAtLeast(80)
                            val heightDp = resolvedHeightDp
                            val currentDims = (widthDp to heightDp)
                            if (hostView.tag != currentDims) {
                                val options = android.os.Bundle().apply {
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                                }
                                try {
                                    hostView.updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp)
                                    appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
                                } catch (e: Exception) {}
                                hostView.tag = currentDims
                            }
                            if (!hasSnapshotSaved) {
                                hasSnapshotSaved = true
                                hostView.post {
                                    WidgetSnapshotManager.saveSnapshotAsync(hostView.context, appWidgetId, hostView)
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore view update failures
                        }
                    },
                    onRelease = { hostView ->
                        (hostView.parent as? ViewGroup)?.removeView(hostView)
                    }
                )
            }
        }

        if (isEditing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures {}
                    }
            )
        }
    }
}


