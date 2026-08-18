package com.amora.companion

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.amora.companion.core.system.overlay.ACTION_WAKE_WORD_DETECTED
import com.amora.companion.core.system.master.MasterSwitchManager
import com.amora.companion.core.system.permissions.PermissionManager
import com.amora.companion.core.theme.AmoraThemeSystem
import com.amora.companion.feature.callassistant.CallAssistantViewModel
import com.amora.companion.feature.home.DashboardViewModel
import com.amora.companion.feature.profile.ProfileViewModel
import com.amora.companion.feature.settings.SettingsViewModel
import com.amora.companion.navigation.AmoraNavHost
import com.amora.companion.navigation.AmoraScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var masterSwitchManager: MasterSwitchManager

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val callAssistantViewModel: CallAssistantViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private var navigateTo: ((String) -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val camGranted = permissions[Manifest.permission.CAMERA] ?: false
        Log.i(TAG, "Permissions result — MIC: $micGranted, CAM: $camGranted")
    }

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_WAKE_WORD_DETECTED) {
                Log.i(TAG, "Wake word broadcast received — navigating to Dashboard")
                navigateTo?.invoke(AmoraScreen.Dashboard.route)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this,
            wakeWordReceiver,
            IntentFilter(ACTION_WAKE_WORD_DETECTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(wakeWordReceiver)
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestStartupPermissions()
        masterSwitchManager.observeAndStartOnBoot(lifecycleScope)

        setContent {
            var currentRoute by remember { mutableStateOf(AmoraScreen.Dashboard.route) }
            val backStack = remember { mutableStateListOf<String>() }

            // Chrome browser style navigation backstack handler
            fun navigateToRoute(targetRoute: String) {
                if (targetRoute != currentRoute) {
                    backStack.add(currentRoute)
                    currentRoute = targetRoute
                }
            }

            BackHandler(enabled = backStack.isNotEmpty()) {
                val previousRoute = backStack.removeAt(backStack.lastIndex)
                currentRoute = previousRoute
            }

            navigateTo = { route -> navigateToRoute(route) }
            val themeName by settingsViewModel.themePreset.collectAsState(initial = "Midnight Void")
            val wallpaperUriStr by settingsViewModel.customWallpaperUri.collectAsState(initial = "")
            val currentPalette = remember(themeName) { AmoraThemeSystem.getPalette(themeName) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = currentPalette.backgroundColor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Ambient Glow Background matching Figma App UI
                    com.amora.companion.core.theme.AmbientBg()

                    if (wallpaperUriStr.isNotEmpty()) {
                        AsyncImage(
                            model = wallpaperUriStr,
                            contentDescription = "Custom Wallpaper",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                    }

                    // Navigation Tabs
                    val navTabs = remember {
                        listOf(
                            Triple(AmoraScreen.Dashboard.route, "Home", "🏠"),
                            Triple(AmoraScreen.Weather.route, "Weather", "🌤️"),
                            Triple(AmoraScreen.News.route, "News", "📰"),
                            Triple(AmoraScreen.Notifications.route, "Notifs", "🔔"),
                            Triple(AmoraScreen.CallAssistant.route, "Calls", "📞"),
                            Triple(AmoraScreen.Alarm.route, "Alarm", "⏰"),
                            Triple(AmoraScreen.PcSpeaker.route, "PC Audio", "📡"),
                            Triple(AmoraScreen.Settings.route, "Settings", "⚙️")
                        )
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            NavigationBar(
                                containerColor = currentPalette.surfaceColor.copy(alpha = 0.9f),
                                modifier = Modifier.height(68.dp)
                            ) {
                                navTabs.forEach { (route, label, emoji) ->
                                    val isSettingsTab = route == AmoraScreen.Settings.route && (
                                        currentRoute == AmoraScreen.Settings.route ||
                                        currentRoute == AmoraScreen.VoiceStudio.route ||
                                        currentRoute == AmoraScreen.Appearance.route ||
                                        currentRoute == AmoraScreen.AISettings.route ||
                                        currentRoute == AmoraScreen.DeviceControlSettings.route ||
                                        currentRoute == AmoraScreen.FilesDocuments.route ||
                                        currentRoute == AmoraScreen.BackendConfig.route ||
                                        currentRoute == AmoraScreen.Permissions.route
                                    )
                                    val isSelected = currentRoute == route || isSettingsTab

                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { navigateToRoute(route) },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) currentPalette.accentColor else currentPalette.subtextColor
                                            )
                                        },
                                        icon = { Text(emoji, fontSize = 15.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = currentPalette.accentColor,
                                            unselectedIconColor = currentPalette.subtextColor,
                                            indicatorColor = currentPalette.accentColor.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AmoraNavHost(
                                currentRoute = currentRoute,
                                onNavigate = { newRoute -> navigateToRoute(newRoute) },
                                dashboardViewModel = dashboardViewModel,
                                settingsViewModel = settingsViewModel,
                                callAssistantViewModel = callAssistantViewModel,
                                profileViewModel = profileViewModel,
                                permissionManager = permissionManager
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestStartupPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
