package com.velocity.launcher.data

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LauncherNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        updateAllActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) {
            instance = null
        }
        _activeNotifications.value = emptyMap()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        updateAllActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        updateAllActiveNotifications()
    }

    private fun updateAllActiveNotifications() {
        try {
            val sbns = activeNotifications ?: emptyArray()
            val models = sbns.mapNotNull { sbnToModel(it) }
            _activeNotifications.value = models.groupBy { it.packageName }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun sbnToModel(sbn: StatusBarNotification): AppNotificationModel? {
        return try {
            val notification = sbn.notification ?: return null
            val extras = notification.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                ?: ""
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: ""

            if (title.isBlank() && text.isBlank()) {
                return null
            }

            AppNotificationModel(
                key = sbn.key,
                packageName = sbn.packageName,
                title = title,
                text = text,
                postTime = sbn.postTime,
                contentIntent = notification.contentIntent,
                isClearable = sbn.isClearable
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private val _activeNotifications = MutableStateFlow<Map<String, List<AppNotificationModel>>>(emptyMap())
        val activeNotifications: StateFlow<Map<String, List<AppNotificationModel>>> = _activeNotifications.asStateFlow()

        private var instance: LauncherNotificationListenerService? = null

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledPackages.contains(context.packageName)
        }

        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }

        fun dismissNotification(key: String) {
            try {
                instance?.cancelNotification(key)
                _activeNotifications.update { map ->
                    map.mapValues { (_, list) ->
                        list.filterNot { it.key == key }
                    }.filterValues { it.isNotEmpty() }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
