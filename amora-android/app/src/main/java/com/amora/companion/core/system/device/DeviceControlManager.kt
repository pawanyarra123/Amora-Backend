package com.amora.companion.core.system.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.amora.companion.core.system.accessibility.AmoraAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceControlManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun adjustVolume(deltaPercent: Int) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVol = (currentVol + (maxVol * (deltaPercent / 100.0))).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
    }

    fun setVolumeLevel(percent: Int) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVol = (maxVol * (percent / 100.0)).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
    }

    fun toggleFlashlight(enabled: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleWifi(enable: Boolean?): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val targetState = enable ?: !wifiManager.isWifiEnabled

            @Suppress("DEPRECATION")
            val success = wifiManager.setWifiEnabled(targetState)

            if (!success) {
                // If direct toggle is blocked by modern Android Q+, open panel and auto-click switch via accessibility
                openWifiPanel()
                AmoraAccessibilityService.instance?.let { service ->
                    Handler(Looper.getMainLooper()).postDelayed({
                        service.clickText("Use Wi‑Fi") || service.clickText("Wi-Fi") || service.clickText("Internet")
                    }, 500)
                }
            }
            targetState
        } catch (e: Exception) {
            openWifiPanel()
            enable ?: true
        }
    }

    fun toggleBluetooth(enable: Boolean?): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            @Suppress("DEPRECATION")
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (adapter != null) {
                val targetState = enable ?: !adapter.isEnabled
                @Suppress("DEPRECATION")
                if (targetState) {
                    adapter.enable()
                } else {
                    adapter.disable()
                }
                targetState
            } else {
                openBluetoothPanel()
                enable ?: true
            }
        } catch (e: Exception) {
            openBluetoothPanel()
            enable ?: true
        }
    }

    fun openWifiPanel() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openBluetoothPanel() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openApp(packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }
}
