package com.amora.companion.core.assistant.actions

import android.content.Context
import com.amora.companion.core.assistant.intent.AssistantIntent
import com.amora.companion.core.system.device.DeviceControlManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceControlManager: DeviceControlManager
) : IActionHandler {

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.SetFlashlight ||
                intent is AssistantIntent.SetVolume ||
                intent is AssistantIntent.AdjustVolume ||
                intent is AssistantIntent.ToggleWifi ||
                intent is AssistantIntent.ToggleBluetooth ||
                intent is AssistantIntent.OpenWifiSettings ||
                intent is AssistantIntent.OpenBluetoothSettings ||
                intent is AssistantIntent.GetTime ||
                intent is AssistantIntent.GetDate
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        return when (intent) {
            is AssistantIntent.SetFlashlight -> {
                deviceControlManager.toggleFlashlight(intent.enabled)
                val stateStr = if (intent.enabled) "on" else "off"
                ActionResult(true, "Flashlight turned $stateStr.")
            }

            is AssistantIntent.ToggleWifi -> {
                val state = deviceControlManager.toggleWifi(intent.enabled)
                val stateStr = when (state) {
                    true -> "turned on"
                    false -> "turned off"
                    null -> "toggled"
                }
                ActionResult(true, "Wi-Fi $stateStr.")
            }

            is AssistantIntent.ToggleBluetooth -> {
                val state = deviceControlManager.toggleBluetooth(intent.enabled)
                val stateStr = when (state) {
                    true -> "turned on"
                    false -> "turned off"
                    null -> "toggled"
                }
                ActionResult(true, "Bluetooth $stateStr.")
            }

            is AssistantIntent.SetVolume -> {
                deviceControlManager.setVolumeLevel(intent.percent)
                ActionResult(true, "Volume set to ${intent.percent} percent.")
            }

            is AssistantIntent.AdjustVolume -> {
                deviceControlManager.adjustVolume(intent.deltaPercent)
                val direction = if (intent.deltaPercent > 0) "increased" else "decreased"
                ActionResult(true, "Volume $direction.")
            }

            is AssistantIntent.OpenWifiSettings -> {
                deviceControlManager.openWifiPanel()
                ActionResult(true, "Opening Wi-Fi settings.")
            }

            is AssistantIntent.OpenBluetoothSettings -> {
                deviceControlManager.openBluetoothPanel()
                ActionResult(true, "Opening Bluetooth settings.")
            }

            is AssistantIntent.GetTime -> {
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                ActionResult(true, "The current time is $timeStr.")
            }

            is AssistantIntent.GetDate -> {
                val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                ActionResult(true, "Today is $dateStr.")
            }

            else -> ActionResult(false, "Unsupported device action.")
        }
    }
}
