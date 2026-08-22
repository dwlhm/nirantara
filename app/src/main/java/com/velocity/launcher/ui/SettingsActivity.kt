package com.velocity.launcher.ui

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.velocity.launcher.R
import com.velocity.launcher.data.PreferencesManager
import com.velocity.launcher.data.ScrollbarPosition
import com.velocity.launcher.data.ThemeMode
import com.velocity.launcher.util.DefaultLauncherHelper

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        preferencesManager = PreferencesManager(this)

        val themeStyle = when (preferencesManager.themeMode) {
            ThemeMode.ADAPTIVE, ThemeMode.SYSTEM -> R.style.Theme_NirantaraLauncher
            ThemeMode.LIGHT -> R.style.Theme_NirantaraLauncher_Light
            ThemeMode.DARK -> R.style.Theme_NirantaraLauncher_Dark
            ThemeMode.SOLID_COLOR -> R.style.Theme_NirantaraLauncher_AmoledBlack
        }
        setTheme(themeStyle)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupThemeRadioGroup()
        setupScrollbarPositionRadioGroup()
        setupHapticsSwitch()
        setupSetDefaultLauncherButton()
    }

    private fun setupThemeRadioGroup() {
        val rgTheme: RadioGroup = findViewById(R.id.rg_theme)
        val rbAdaptive: RadioButton = findViewById(R.id.rb_theme_adaptive)
        val rbDark: RadioButton = findViewById(R.id.rb_theme_dark)
        val rbLight: RadioButton = findViewById(R.id.rb_theme_light)
        val rbSystem: RadioButton = findViewById(R.id.rb_theme_system)
        val rbSolid: RadioButton = findViewById(R.id.rb_theme_solid_color)

        when (preferencesManager.themeMode) {
            ThemeMode.ADAPTIVE -> rbAdaptive.isChecked = true
            ThemeMode.DARK -> rbDark.isChecked = true
            ThemeMode.LIGHT -> rbLight.isChecked = true
            ThemeMode.SYSTEM -> rbSystem.isChecked = true
            ThemeMode.SOLID_COLOR -> rbSolid.isChecked = true
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selectedTheme = when (checkedId) {
                R.id.rb_theme_adaptive -> ThemeMode.ADAPTIVE
                R.id.rb_theme_dark -> ThemeMode.DARK
                R.id.rb_theme_light -> ThemeMode.LIGHT
                R.id.rb_theme_system -> ThemeMode.SYSTEM
                R.id.rb_theme_solid_color -> ThemeMode.SOLID_COLOR
                else -> ThemeMode.ADAPTIVE
            }
            if (preferencesManager.themeMode != selectedTheme) {
                preferencesManager.themeMode = selectedTheme
                recreate()
            }
        }
    }

    private fun setupScrollbarPositionRadioGroup() {
        val rgPos: RadioGroup = findViewById(R.id.rg_scrollbar_pos)
        val rbRight: RadioButton = findViewById(R.id.rb_pos_right)
        val rbLeft: RadioButton = findViewById(R.id.rb_pos_left)
        val rbBoth: RadioButton = findViewById(R.id.rb_pos_both)

        when (preferencesManager.scrollbarPosition) {
            ScrollbarPosition.RIGHT -> rbRight.isChecked = true
            ScrollbarPosition.LEFT -> rbLeft.isChecked = true
            ScrollbarPosition.BOTH -> rbBoth.isChecked = true
        }

        rgPos.setOnCheckedChangeListener { _, checkedId ->
            val selectedPos = when (checkedId) {
                R.id.rb_pos_left -> ScrollbarPosition.LEFT
                R.id.rb_pos_both -> ScrollbarPosition.BOTH
                else -> ScrollbarPosition.RIGHT
            }
            preferencesManager.scrollbarPosition = selectedPos
        }
    }

    private fun setupHapticsSwitch() {
        val switchHaptics: SwitchMaterial = findViewById(R.id.switch_haptics)
        switchHaptics.isChecked = preferencesManager.hapticFeedbackEnabled
        switchHaptics.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.hapticFeedbackEnabled = isChecked
        }
    }

    private fun setupSetDefaultLauncherButton() {
        val btnSetDefault: MaterialButton = findViewById(R.id.btn_set_default_launcher)
        btnSetDefault.setOnClickListener {
            openDefaultLauncherSettings()
        }
    }

    private fun openDefaultLauncherSettings() {
        DefaultLauncherHelper.openDefaultLauncherSettings(this)
    }
}
