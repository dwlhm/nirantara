package com.velocity.launcher.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.velocity.launcher.R
import com.velocity.launcher.data.DisplayMode
import com.velocity.launcher.data.PreferencesManager
import com.velocity.launcher.data.ThemeMode

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        preferencesManager = PreferencesManager(this)

        val themeStyle = when (preferencesManager.themeMode) {
            ThemeMode.LIGHT -> R.style.Theme_VelocityLauncher_Light
            ThemeMode.DARK -> R.style.Theme_VelocityLauncher_Dark
            ThemeMode.AMOLED_BLACK -> R.style.Theme_VelocityLauncher_AmoledBlack
        }
        setTheme(themeStyle)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupThemeRadioGroup()
        setupDisplayModeRadioGroup()
        setupGridColumnsRadioGroup()
        setupSetDefaultLauncherButton()
    }

    private fun setupThemeRadioGroup() {
        val rgTheme: RadioGroup = findViewById(R.id.rg_theme)
        val rbLight: RadioButton = findViewById(R.id.rb_theme_light)
        val rbDark: RadioButton = findViewById(R.id.rb_theme_dark)
        val rbAmoled: RadioButton = findViewById(R.id.rb_theme_amoled)

        when (preferencesManager.themeMode) {
            ThemeMode.LIGHT -> rbLight.isChecked = true
            ThemeMode.DARK -> rbDark.isChecked = true
            ThemeMode.AMOLED_BLACK -> rbAmoled.isChecked = true
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selectedTheme = when (checkedId) {
                R.id.rb_theme_dark -> ThemeMode.DARK
                R.id.rb_theme_amoled -> ThemeMode.AMOLED_BLACK
                else -> ThemeMode.LIGHT
            }
            if (preferencesManager.themeMode != selectedTheme) {
                preferencesManager.themeMode = selectedTheme
                recreate()
            }
        }
    }

    private fun setupDisplayModeRadioGroup() {
        val rgDisplay: RadioGroup = findViewById(R.id.rg_display_mode)
        val rbGrid: RadioButton = findViewById(R.id.rb_mode_grid)
        val rbText: RadioButton = findViewById(R.id.rb_mode_text)

        when (preferencesManager.displayMode) {
            DisplayMode.GRID -> rbGrid.isChecked = true
            DisplayMode.TEXT_ONLY -> rbText.isChecked = true
        }

        rgDisplay.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = when (checkedId) {
                R.id.rb_mode_text -> DisplayMode.TEXT_ONLY
                else -> DisplayMode.GRID
            }
            preferencesManager.displayMode = selectedMode
        }
    }

    private fun setupGridColumnsRadioGroup() {
        val rgColumns: RadioGroup = findViewById(R.id.rg_columns)
        val rb3: RadioButton = findViewById(R.id.rb_cols_3)
        val rb4: RadioButton = findViewById(R.id.rb_cols_4)
        val rb5: RadioButton = findViewById(R.id.rb_cols_5)

        when (preferencesManager.gridColumnCount) {
            3 -> rb3.isChecked = true
            4 -> rb4.isChecked = true
            5 -> rb5.isChecked = true
            else -> rb4.isChecked = true
        }

        rgColumns.setOnCheckedChangeListener { _, checkedId ->
            val cols = when (checkedId) {
                R.id.rb_cols_3 -> 3
                R.id.rb_cols_5 -> 5
                else -> 4
            }
            preferencesManager.gridColumnCount = cols
        }
    }

    private fun setupSetDefaultLauncherButton() {
        val btnSetDefault: MaterialButton = findViewById(R.id.btn_set_default_launcher)
        btnSetDefault.setOnClickListener {
            openDefaultLauncherSettings()
        }
    }

    private fun openDefaultLauncherSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME), 1001)
                    return
                } catch (e: Exception) {
                    // Fallback to Settings.ACTION_HOME_SETTINGS
                }
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (ex: Exception) {
                // Ignore if unavailable
            }
        }
    }

}
