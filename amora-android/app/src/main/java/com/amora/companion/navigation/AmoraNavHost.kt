package com.amora.companion.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.amora.companion.core.system.permissions.PermissionManager
import com.amora.companion.feature.alarm.AlarmScreen
import com.amora.companion.feature.callassistant.CallAssistantScreen
import com.amora.companion.feature.callassistant.CallAssistantViewModel
import com.amora.companion.feature.emergency.EmergencyScreen
import com.amora.companion.feature.help.HelpSupportScreen
import com.amora.companion.feature.home.DashboardScreen
import com.amora.companion.feature.home.DashboardViewModel
import com.amora.companion.feature.news.NewsScreen
import com.amora.companion.feature.notifications.NotificationCenterScreen
import com.amora.companion.feature.pcspeaker.PcSpeakerScreen
import com.amora.companion.feature.profile.ProfileScreen
import com.amora.companion.feature.profile.ProfileViewModel
import com.amora.companion.feature.settings.*
import com.amora.companion.feature.weather.WeatherScreen

sealed class AmoraScreen(val route: String) {
    object Dashboard : AmoraScreen("dashboard")
    object Weather : AmoraScreen("weather")
    object News : AmoraScreen("news")
    object Notifications : AmoraScreen("notifications")
    object CallAssistant : AmoraScreen("call_assistant")
    object Settings : AmoraScreen("settings")
    object VoiceStudio : AmoraScreen("voice_studio")
    object Appearance : AmoraScreen("appearance")
    object AISettings : AmoraScreen("ai_settings")
    object DeviceControlSettings : AmoraScreen("device_control_settings")
    object FilesDocuments : AmoraScreen("files_documents")
    object BackendConfig : AmoraScreen("backend_config")
    object Permissions : AmoraScreen("permissions")
    object Emergency : AmoraScreen("emergency")
    object Profile : AmoraScreen("profile")
    object HelpSupport : AmoraScreen("help_support")
    object Alarm : AmoraScreen("alarm")
    object PcSpeaker : AmoraScreen("pc_speaker")
}

@Composable
fun AmoraNavHost(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    dashboardViewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel,
    callAssistantViewModel: CallAssistantViewModel,
    profileViewModel: ProfileViewModel,
    permissionManager: PermissionManager
) {
    val themeName by settingsViewModel.themePreset.collectAsState(initial = "Cyberpunk Neon")
    val navigateBackToSettings = { onNavigate(AmoraScreen.Settings.route) }

    when (currentRoute) {
        AmoraScreen.Dashboard.route -> DashboardScreen(viewModel = dashboardViewModel, onNavigate = onNavigate, themeName = themeName)
        AmoraScreen.Weather.route -> WeatherScreen(themeName = themeName)
        AmoraScreen.News.route -> NewsScreen(themeName = themeName)
        AmoraScreen.Notifications.route -> NotificationCenterScreen(themeName = themeName)
        AmoraScreen.CallAssistant.route -> CallAssistantScreen(viewModel = callAssistantViewModel, themeName = themeName)
        AmoraScreen.Settings.route -> SettingsScreen(viewModel = settingsViewModel, onNavigate = onNavigate)
        AmoraScreen.VoiceStudio.route -> VoiceStudioScreen(onBackClick = navigateBackToSettings, themeName = themeName)
        AmoraScreen.Appearance.route -> AppearanceScreen(viewModel = settingsViewModel, onBackClick = navigateBackToSettings)
        AmoraScreen.AISettings.route -> AISettingsScreen(onBackClick = navigateBackToSettings)
        AmoraScreen.DeviceControlSettings.route -> DeviceControlSettingsScreen(onBackClick = navigateBackToSettings)
        AmoraScreen.FilesDocuments.route -> FilesDocumentsScreen(onBackClick = navigateBackToSettings)
        AmoraScreen.BackendConfig.route -> BackendConfigScreen(viewModel = settingsViewModel, onBackClick = navigateBackToSettings, themeName = themeName)
        AmoraScreen.Permissions.route -> PermissionSetupScreen(permissionManager = permissionManager, onBackClick = navigateBackToSettings, themeName = themeName)
        AmoraScreen.Emergency.route -> EmergencyScreen()
        AmoraScreen.Profile.route -> ProfileScreen(viewModel = profileViewModel)
        AmoraScreen.HelpSupport.route -> HelpSupportScreen()
        AmoraScreen.Alarm.route -> AlarmScreen(themeName = themeName)
        AmoraScreen.PcSpeaker.route -> PcSpeakerScreen()
        else -> DashboardScreen(viewModel = dashboardViewModel, onNavigate = onNavigate, themeName = themeName)
    }
}
