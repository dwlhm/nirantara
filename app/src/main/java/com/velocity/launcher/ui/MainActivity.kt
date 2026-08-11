package com.velocity.launcher.ui

import android.appwidget.AppWidgetHost
import com.velocity.launcher.ui.widget.ScrollAwareAppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.velocity.launcher.ui.compose.LauncherScreen
import com.velocity.launcher.ui.compose.LauncherViewModel
import com.velocity.launcher.ui.theme.VelocityLauncherTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LauncherViewModel
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost

    companion object {
        const val APPWIDGET_HOST_ID = 1024
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing
            }
        })
        enableEdgeToEdge()
        
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = ScrollAwareAppWidgetHost(this, APPWIDGET_HOST_ID)

        viewModel = ViewModelProvider(
            this, 
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[LauncherViewModel::class.java]

        setContent {
            VelocityLauncherTheme {
                LauncherScreen(
                    viewModel = viewModel,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

}
