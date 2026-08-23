package com.velocity.launcher.ui.compose.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.velocity.launcher.ui.widget.ScrollAwareAppWidgetHostView

@Composable
fun WidgetHostContainer(
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    span: Int = 8,
    customHeightDp: Int? = null,
    isEditing: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (appWidgetId == -1) return

    val appWidgetInfo = remember(appWidgetId) {
        appWidgetManager.getAppWidgetInfo(appWidgetId)
    }

    if (appWidgetInfo == null) return

    val heightModifier = if (customHeightDp != null && customHeightDp > 0) {
        Modifier.height(customHeightDp.dp)
    } else {
        Modifier.wrapContentHeight()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier),
        contentAlignment = Alignment.Center
    ) {
        key(appWidgetId) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(heightModifier),
                factory = { context: Context ->
                    try {
                        val hostView = appWidgetHost.createView(context, appWidgetId, appWidgetInfo)
                        if (hostView is ScrollAwareAppWidgetHostView) {
                            hostView.onLongPressListener = onLongPress
                        }
                        hostView.layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        hostView.setAppWidget(appWidgetId, appWidgetInfo)
                        hostView.setPadding(0, 0, 0, 0)
                        
                        val density = context.resources.displayMetrics.density
                        val screenWidthPx = context.resources.displayMetrics.widthPixels
                        val totalWidthDp = (screenWidthPx / density).coerceAtLeast(100f)
                        val widthDp = ((totalWidthDp * (span / 8.0f)).toInt()).coerceAtLeast(80)
                        val heightDp = customHeightDp ?: (appWidgetInfo.minHeight).coerceAtLeast(80)
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
                        AppWidgetHostView(context)
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
                        val heightDp = customHeightDp ?: (appWidgetInfo.minHeight).coerceAtLeast(80)
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
                    } catch (e: Exception) {
                        // Ignore view update failures
                    }
                },
                onRelease = { hostView ->
                    (hostView.parent as? ViewGroup)?.removeView(hostView)
                }
            )
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

