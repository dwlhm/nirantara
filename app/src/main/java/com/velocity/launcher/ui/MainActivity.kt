package com.velocity.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import com.velocity.launcher.ui.compose.LauncherScreen
import com.velocity.launcher.ui.compose.LauncherViewModel
import com.velocity.launcher.ui.theme.NirantaraTheme
import com.velocity.launcher.ui.widget.ScrollAwareAppWidgetHost

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LauncherViewModel
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost

    companion object {
        const val APPWIDGET_HOST_ID = 1024
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wallpaper window flags
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        enableEdgeToEdge()

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = ScrollAwareAppWidgetHost(this, APPWIDGET_HOST_ID)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[LauncherViewModel::class.java]

        handleLaunchIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = viewModel.state.value
                when {
                    state.isHiddenAppsOpen -> viewModel.closeHiddenAppsScreen()
                    state.isSettingsOpen -> viewModel.closeSettingsScreen()
                    state.isSearchOpen -> viewModel.closeSearch()
                    state.isWidgetPickerOpen -> viewModel.closeWidgetPicker()
                    state.activeBottomSheetApp != null -> viewModel.closeBottomSheet()
                    state.activePopupApp != null -> viewModel.closePopup()
                    state.activeEditingContainerKey != null -> viewModel.setActiveEditingContainer(null)
                    else -> {
                        // Stay on launcher home
                    }
                }
            }
        })

        setContent {
            val state by viewModel.state.collectAsState()
            NirantaraTheme(
                backgroundType = state.backgroundType,
                colorTone = state.colorTone,
                themeMode = state.themeMode,
                adaptiveAccentColor = state.adaptiveAccentColor?.toArgb(),
                solidBackgroundColor = state.solidBackgroundColor.toArgb(),
                solidAccentColor = state.solidAccentColor.toArgb()
            ) {
                LauncherScreen(
                    viewModel = viewModel,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        val isLauncherAction = intent.action == Intent.ACTION_MAIN
        val hasLauncherCategory = intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        val hasHomeCategory = intent.hasCategory(Intent.CATEGORY_HOME)

        if (isLauncherAction && hasLauncherCategory && !hasHomeCategory) {
            viewModel.openSettingsScreen()
        } else if (hasHomeCategory) {
            viewModel.closeSettingsScreen()
            viewModel.closeHiddenAppsScreen()
            viewModel.closeSearch()
            viewModel.closeBottomSheet()
            viewModel.closePopup()
            viewModel.closeWidgetPicker()
        }
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadApps()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }
}

