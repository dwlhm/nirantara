package com.velocity.launcher.data

import android.app.PendingIntent
import android.os.UserHandle
import androidx.compose.runtime.Immutable

@Immutable
data class AppModel(
    val label: String,
    val packageName: String,
    val className: String,
    val userHandle: UserHandle,
    val isWorkProfile: Boolean = false,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isWidgetExposedInline: Boolean = false,
    val popupWidgetIds: List<Int> = emptyList(),
    val id: String = "${packageName}/${className}#${userHandle.hashCode()}",
    val normalizedLabel: String = "",
    val wordPrefixes: List<String> = emptyList(),
    val initialisms: List<String> = emptyList()
) {
    val popupWidgetId: Int? get() = popupWidgetIds.firstOrNull()
}

@Immutable
data class AppNotificationModel(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val contentIntent: PendingIntent? = null,
    val isClearable: Boolean = true
)


