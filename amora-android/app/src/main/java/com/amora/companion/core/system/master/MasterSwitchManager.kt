package com.amora.companion.core.system.master

import android.content.Context
import android.content.Intent
import android.os.Build
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import com.amora.companion.core.system.overlay.FloatingOrbService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterSwitchManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    val isMasterSwitchOn: Flow<Boolean> = preferencesRepository.isMasterSwitchOn

    fun observeAndStartOnBoot(scope: CoroutineScope) {
        scope.launch {
            isMasterSwitchOn.collect { isEnabled ->
                val intent = Intent(context, FloatingOrbService::class.java)
                if (isEnabled) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        context.stopService(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    suspend fun setMasterSwitch(enabled: Boolean) {
        preferencesRepository.setMasterSwitchOn(enabled)
        val intent = Intent(context, FloatingOrbService::class.java)
        if (enabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
