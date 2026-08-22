package com.velocity.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Immutable
data class BatteryState(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val isPlugged: Boolean = false
)

class BatteryMonitor(private val context: Context) {

    val batteryState: Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

                    val batteryPct = if (level >= 0 && scale > 0) {
                        (level * 100 / scale.toFloat()).toInt()
                    } else {
                        100
                    }

                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    val isPlugged = plugged > 0

                    trySend(
                        BatteryState(
                            level = batteryPct,
                            isCharging = isCharging,
                            isPlugged = isPlugged
                        )
                    )
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(receiver, filter)
        if (initialIntent != null) {
            val level = initialIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = initialIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = initialIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = initialIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

            val batteryPct = if (level >= 0 && scale > 0) {
                (level * 100 / scale.toFloat()).toInt()
            } else {
                100
            }

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val isPlugged = plugged > 0

            trySend(
                BatteryState(
                    level = batteryPct,
                    isCharging = isCharging,
                    isPlugged = isPlugged
                )
            )
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver might already be unregistered
            }
        }
    }
}

