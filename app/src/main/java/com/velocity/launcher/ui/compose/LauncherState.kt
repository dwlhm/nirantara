package com.velocity.launcher.ui.compose

import android.content.pm.ShortcutInfo
import com.velocity.launcher.data.AppModel
import java.util.UUID

data class CardState(
    val id: String = UUID.randomUUID().toString(),
    val app: AppModel,
    val widthCells: Int = 1,
    val heightCells: Int = 1,
    val isWidgetConfigured: Boolean = false,
    val appWidgetId: Int? = null,
    val shortcuts: List<ShortcutInfo> = emptyList()
)

data class FolderState(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val cards: List<CardState>
)

data class LauncherState(
    val folders: List<FolderState> = emptyList(),
    val neutralApps: List<AppModel> = emptyList(),
    val isLoading: Boolean = true
)
