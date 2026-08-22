package com.velocity.launcher.ui.compose.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private val hostViewCache = mutableMapOf<Int, AppWidgetHostView>()

@Composable
fun WidgetHostContainer(
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    customHeightDp: Int? = null,
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
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .then(heightModifier),
            factory = { context: Context ->
                try {
                    val cachedView = hostViewCache[appWidgetId]
                    val hostView = if (cachedView != null) {
                        (cachedView.parent as? ViewGroup)?.removeView(cachedView)
                        cachedView
                    } else {
                        val newView = appWidgetHost.createView(context, appWidgetId, appWidgetInfo)
                        hostViewCache[appWidgetId] = newView
                        newView
                    }
                    hostView.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    hostView.setAppWidget(appWidgetId, appWidgetInfo)
                    
                    val density = context.resources.displayMetrics.density
                    val widthPx = context.resources.displayMetrics.widthPixels
                    val widthDp = (widthPx / density).toInt()
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

                    hostView
                } catch (e: Exception) {
                    AppWidgetHostView(context)
                }
            },
            update = { hostView ->
                try {
                    hostView.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    hostView.setAppWidget(appWidgetId, appWidgetInfo)
                    val density = hostView.context.resources.displayMetrics.density
                    val widthPx = hostView.context.resources.displayMetrics.widthPixels
                    val widthDp = (widthPx / density).toInt()
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
                } catch (e: Exception) {
                    // Ignore view update failures
                }
            }
        )
    }
}

